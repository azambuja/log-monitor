oil = require "oil"

-- OiL configuration
local orb = oil.init({host = "localhost", port = 20287})
oil.orb = orb

orb:loadidlfile("../../../../../idl/deployment.idl")
orb:loadidlfile("../../../../../idl/repository.idl")
orb:loadidlfile("../../../../../idl/pingPong.idl")

oil.main(function()
	local lua_name = "lua"
	local path = "../../execution_node/"
	-- creating a new Execution Node
	-- it opens a new prompt window
--[[
	os.execute("start " .. lua_name .. " " .. path .. "ExecutionNode.lua")
	oil.sleep(1)
--]]
	local EN = orb:newproxy(assert(oil.readfrom(path .. "execution_node.ior")))
	local status, err = oil.pcall(EN.startup, EN)
	if not status then
		print(err)
		return
	end

	local status, enFacet = oil.pcall(EN.getFacet, EN, "IDL:scs/execution_node/ExecutionNode:1.0")
	if not status then
		print("[PingPong] Error while calling getFacet(IDL:scs/execution_node/ExecutionNode:1.0)")
		print("[PingPong] Error: " .. enFacet)
		return
	end
	enFacet = orb:narrow(enFacet)

	-- creating container
	local containerName = "ComponentContainer"
	local containerPropertySeq = { { name = "language" , value = "java", read_only = false }, { name = "classpath", value = "..//..//..//java", read_only = false } }

	local status, CC = oil.pcall(enFacet.startContainer, enFacet, containerName, containerPropertySeq)
	if not status then
			print("[PingPong] Error while calling startContainer!")
			print("[PingPong] Error: " .. CC)
			return
	end

	CC:startup()
	local clFacet = CC:getFacet("scs::container::ComponentLoader")
	clFacet = orb:narrow(clFacet, "scs::container::ComponentLoader")

 	print("Finished configuration process.")

	-- loading pingpongs
	local pp1Handle = clFacet:load({ name = "PingPong", version = 1.0 }, {})
	pp1Handle.cmp:startup()
	local pp2Handle = clFacet:load({ name = "PingPong", version = 1.0 }, {})
	pp2Handle.cmp:startup()
	local pp1Rec = pp1Handle.cmp:getFacet("scs::core::IReceptacles")
	pp1Rec = orb:narrow(pp1Rec, "scs::core::IReceptacles")
	local pp2Rec = pp2Handle.cmp:getFacet("scs::core::IReceptacles")
	pp2Rec = orb:narrow(pp2Rec, "scs::core::IReceptacles")
	local pp1 = pp1Handle.cmp:getFacet("scs::demos::pingpong::PingPong")
	pp1 = orb:narrow(pp1, "scs::demos::pingpong::PingPong")
 	local pp2 = pp2Handle.cmp:getFacet("scs::demos::pingpong::PingPong")
	pp2 = orb:narrow(pp2, "scs::demos::pingpong::PingPong")

	-- connecting them
	pp1Rec:connect("PingPong", pp2)
	pp2Rec:connect("PingPong", pp1)

	-- java pingpongs must all be started
	pp1:setId(1)
	pp2:setId(2)
 	pp2:start()
 	pp1:start()

	print("Pings and Pongs started!")
end)
