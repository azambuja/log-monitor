--
-- SCS - Software Component System
-- ServerInterceptorsManager.lua
-- Description: ServerInterceptorsManager component implementation
-- Version: 1.0
--

local oil		= require "oil"
local oo		= require "loop.base"
local comp	= require "loop.component.base"
local port		= require "loop.component.base"
local scs		= require "scs.core.base"
local utils		= require "scs.core.utils"

-- local Log = require "openbus.common.Log"
-- Log:level(3)
local Log = utils.Utils()
Log.interceptor = Log.verbosePrint
Log.verbose = false

local print		= print
local pairs		= pairs
local ipairs		= ipairs
local tostring  		= tostring
local tonumber	= tonumber
local dofile		= dofile
local string		= string
local assert		= assert
local os        		= os
local loadfile  		= loadfile
local coroutine		= coroutine
local error		= error
local table		= table

--------------------------------------------------------------------------------
module "scs.interceptor.InterceptorsManager"
--------------------------------------------------------------------------------

--------------------------------------------------------------------------------
-- InterceptorsManager Class
--------------------------------------------------------------------------------

-- list: (ordered)
-- [1] = handle
-- listById:
-- [id] = 1

local IM = oo.class{
	props = {},
	clientList = {},
	clientListById = {},
	serverList = {},
	serverListById = {},
	clientReadCounter = 0,
	serverReadCounter = 0,
	clientWriteLocked = false,
	serverWriteLocked = false,
	-- suspension
	container = false,
	coroutines = {},
	-- behaviour: 
	-- 0 means no suspension
	-- a positive number means a long suspension (launches exception)
	-- a negative number means a short suspension (yields coroutines)
	behaviour = 0,
}

function IM:__init(container, behaviour)
	-- makes sure that table values are recreated for the instance. If not, base class values 
	-- may be modified.
  Log:interceptor("Constructing interceptors manager")
	local inst = oo.rawnew(self, {})
	inst.props = {}
	inst.clientList = {}
	inst.clientListById = {}
	inst.serverList = {}
	inst.serverListById = {}
	utils.Utils:readProperties(inst.props, "../interceptor/IMConfig.txt")
	-- suspension
	inst.coroutines = {}
	inst.container = container
	inst.behaviour = behaviour or inst.behaviour
	return inst
end


--
-- Intercepts requests from any component to call interceptors
--
function IM:sendrequest(request)
  Log:interceptor("Interceptors manager send request. Behaviour is " .. self:getBehaviourInString() .. ".")
  Log:interceptor("Interceptors manager: Request for operation " .. request.operation .. ".")

	-- liberates container's openbus threads and openbus interceptors
	local containerThread = false
	if self.container.props.use_openbus == "true" then
		containerThread = self.container.connectionManager.leaseHolder.timer.thread
	end

	-- also liberates calls to container servants and openbus server interceptor using activeServant
	if not self.container.activeServant[oil.tasks.current] and oil.tasks.current ~= containerThread then
		if self.behaviour < 0 then
		  Log:interceptor("Interceptors manager: Yielding request to be sent!")
			table.insert(self.coroutines, oil.tasks.current)
			oil.tasks:suspend()
		  Log:interceptor("Interceptors manager: Resuming send request!")
		else
			if self.behaviour > 0 then
			  Log:interceptor("Interceptors manager: Launching ContainerHalted exception.")
				error{"IDL:scs/container/ContainerHalted:1.0"}
			end
		end
	end

	if not self:readLockList("client") then
		self.clientReadCounter = self.clientReadCounter + 1
		return
	end
	Log:interceptor("Interceptors manager: Interceptor client list now being used for interceptor calls.")
	for _, handle in ipairs(self.clientList) do
		local interceptor = handle.cmp.context
		local status, err = oil.pcall(interceptor.sendrequest, interceptor, request)
		if not status then
			Log:interceptor("Send request error in interceptor " .. handle.id.name .. ": " .. err)
			self.clientReadCounter = self.clientReadCounter - 1
			Log:interceptor("Request not sent, reply will not be received, client list liberated by this call.")
			Log:interceptor("Client interceptor calls aborted, launching catched error.")
			error(err)
		end
-- 		interceptor:sendrequest(request)
	end
-- 	self.clientReadCounter = self.clientReadCounter - 1
	Log:interceptor("Client interceptor calls completed.")
end

--
-- Intercepts the received reply to call interceptors
--
function IM:receivereply(reply)
  Log:interceptor("Interceptors manager receive reply")
-- 	if not self:readLockList("client") then
-- 		return
-- 	end
-- 	Log:interceptor("Interceptor client list now being used for interceptor calls.")
	for _, handle in ipairs(self.clientList) do
		local interceptor = handle.cmp.context
		local status, err = oil.pcall(interceptor.receivereply, interceptor, reply)
		if not status then
			Log:interceptor("Receive reply error in interceptor " .. handle.id.name .. ": " .. err)
			self.clientReadCounter = self.clientReadCounter - 1
			Log:interceptor("Request sent and reply received but with interceptor errors, client list liberated by this call.")
			Log:interceptor("Client interceptor calls aborted, launching catched error.")
			error(err)
		end
-- 		interceptor:receivereply(reply)
	end
	self.clientReadCounter = self.clientReadCounter - 1
	Log:interceptor("Request sent and reply received, client list liberated by this call.")
	Log:interceptor("Client interceptor calls completed.")
end

--
-- Intercepts requests being received to call interceptors
--
function IM:receiverequest(request)
  Log:interceptor("Interceptors manager receive request! Behaviour is " .. self:getBehaviourInString() .. ".")
  Log:interceptor("Interceptors manager: Request for operation " .. request.operation .. ".")

	local obj = request.servant
	self.container.activeServant[oil.tasks.current] = obj
	if obj.context ~= self.container then
		if request.operation ~= "_get_id" then
			if self.behaviour < 0 then
				Log:interceptor("Interceptors manager: Yielding received request!")
				table.insert(self.coroutines, oil.tasks.current)
				oil.tasks:suspend()
				Log:interceptor("Interceptors manager: Received request resumed!")
			else
				if self.behaviour > 0 then
					Log:interceptor("Interceptors manager: Launching ContainerHalted exception.")
					error{"IDL:scs/container/ContainerHalted:1.0"}
				end
			end
		end
	end

	if not self:readLockList("server") then
		self.serverReadCounter = self.serverReadCounter + 1
		return
	end
	Log:interceptor("Interceptor server list now being used for interceptor calls.")
	for _, handle in ipairs(self.serverList) do
		local interceptor = handle.cmp.context
		local status, err = oil.pcall(interceptor.receiverequest, interceptor, request)
		if not status then
			Log:interceptor("Receive request error in interceptor " .. handle.id.name .. ": " .. err)
			self.serverReadCounter = self.serverReadCounter - 1
			Log:interceptor("Request received but not processed, reply will not be sent, server list liberated by this call.")
			Log:interceptor("Server interceptor calls aborted, launching catched error.")
			error(err)
		end
-- 		interceptor:receiverequest(request)
	end
-- 	self.serverReadCounter = self.serverReadCounter - 1
	Log:interceptor("Server interceptor calls completed.")
end

--
-- Intercepts the response to the request to call interceptors
--
function IM:sendreply(reply)
  Log:interceptor("Interceptors manager send reply")

	-- suspension treatment
	self.container.activeServant[oil.tasks.current] = nil

-- 	if not self:readLockList("server") then
-- 		return
-- 	end
-- 	Log:interceptor("Interceptor server list now being used for interceptor calls.")
	for _, handle in ipairs(self.serverList) do
		local interceptor = handle.cmp.context
		local status, err = oil.pcall(interceptor.sendreply, interceptor, reply)
		if not status then
			Log:interceptor("Send reply error in interceptor " .. handle.id.name .. ": " .. err)
			self.serverReadCounter = self.serverReadCounter - 1
			Log:interceptor("Request received and reply with error to be sent, server list liberated by this call.")
			Log:interceptor("Server interceptor calls aborted, launching catched error.")
			error(err)
		end
-- 		interceptor:sendreply(reply)
	end
	self.serverReadCounter = self.serverReadCounter - 1
	Log:interceptor("Request received and reply sent, server list liberated by this call.")
	Log:interceptor("Server interceptor calls completed.")
end


--------------------------------------------------------------------------------
-- Manager Methods
--------------------------------------------------------------------------------

--
-- Locks the interceptor lists for reading
--
function IM:readLockList(type)
	local numWaits = 1
	local maxWaits = tonumber(self.props.timeout) or 10
	while numWaits <= maxWaits do
		if type == "server" then
			if self.serverWriteLocked then
				Log:interceptor("Interceptor server list write locked. Waiting 0.2 second...")
				oil.sleep(0.2)
				numWaits = numWaits + 1
			else
				self.serverReadCounter = self.serverReadCounter + 1
				break
			end
		else
			if self.clientWriteLocked then
				Log:interceptor("Interceptor list write locked. Waiting 0.2 second...")
				oil.sleep(0.2)
				numWaits = numWaits + 1
			else
				self.clientReadCounter = self.clientReadCounter + 1
				break
			end
		end
	end
	if numWaits > maxWaits then
 		Log:interceptor("Interceptor " .. tostring(type) .. " list did not unlock. Call will proceed without executing interceptors.")
		return false
	end
	return true
end

--
-- Description: Locks the lists. Helper function.
--
function IM:lockList(type)
	local numWaits = 1
	local maxWaits = tonumber(self.props.timeout) or 10
	while numWaits <= maxWaits do
		if type == "server" then
			if self.serverReadCounter > 0 or self.serverWriteLocked then
				Log:interceptor("Interceptor server list being used. Waiting 0.5 second...")
				oil.sleep(0.5)
				numWaits = numWaits + 1
			else
				self.serverWriteLocked = true
				break
			end
		else
			if self.clientReadCounter > 0 or self.clientWriteLocked then
				Log:interceptor("Interceptor client list being used. Waiting 0.5 second...")
				oil.sleep(0.5)
				numWaits = numWaits + 1
			else
				self.clientWriteLocked = true
				break
			end
		end
	end
	if numWaits > maxWaits then
		Log:interceptor("Interceptor " .. tostring(type) .. " list could not be locked.")
		return false
	end
	return true
end

function IM:discoverTypeAndPosition(id)
	local type = false
	local pos = -1
	-- read lock on lists
	if not self:readlockList("server") then
		error{"IDL:scs/interceptor/ListLockFail:1.0"};
	end
	if not self:readlockList("client") then
		error{"IDL:scs/interceptor/ListLockFail:1.0"};
	end
	-- discovering type and position
	if self.serverListById[id] == nil then
		if self.clientListById[id] == nil then
			error{"IDL:scs/interceptor/InterceptorNotInstalled:1.0"}
		else
			type = "client"
			pos = self.clientListById[id]
		end
	else
		type = "server"
		pos = self.serverListById[id]
	end
	-- unlocking lists for reading
	self.clientReadCounter = self.clientReadCounter - 1
	self.serverReadCounter = self.serverReadCounter - 1
	return type, pos
end

--
-- Description: Adds a new interceptor to the list.
--
function IM:addInterceptor(handle, position, type)
	Log:interceptor("Add " .. tostring(type) .. " interceptor call")
	if not self:lockList(type) then
		error{"IDL:scs/interceptor/ListLockFail:1.0"};
	end
	Log:interceptor("Interceptor list now locked.")
	if type == "server" then
		if not position or position < 1 or position > #(self.serverList) then
			position = #(self.serverList) + 1
		end
		table.insert(self.serverList, position, handle)
		self.serverListById[handle.instance_id] = position
		self.serverWriteLocked = false
	else
		if not position or position < 1 or position > #(self.clientList) then
			position = #(self.clientList) + 1
		end
		table.insert(self.clientList, position, handle)
		self.clientListById[handle.instance_id] = position
		self.clientWriteLocked = false
	end
	Log:interceptor("Interceptor added and list unlocked.")
end

--
-- Description: Removes an interceptor from the list.
--
function IM:removeInterceptor(id)
	local type, pos = self:discoverTypeAndPosition(id)
	Log:interceptor("Remove " .. tostring(type) .. " interceptor call")
	-- locking list for write
	if not self:lockList(type) then
		error{"IDL:scs/interceptor/ListLockFail:1.0"};
	end
	Log:interceptor("Interceptor list now locked.")
	-- removing interceptor and unlocking list
	if type == "server" then
		if pos == -1 then
			self.serverWriteLocked = false
			error{"IDL:scs/interceptor/InterceptorNotInstalled:1.0"}
		end
		table.remove(self.serverList, pos)
		self.serverListById[id] = nil
		self.serverWriteLocked = false
	else
		if pos == -1 then 
			self.clientWriteLocked = false
			error{"IDL:scs/interceptor/InterceptorNotInstalled:1.0"}
		end
		table.remove(self.clientList, pos)
		self.clientListById[id] = nil
		self.clientWriteLocked = false
	end
	Log:interceptor("Interceptor removed and list unlocked.")
end

--
-- Description: Changes an interceptor position. May not be atomic!
--
function IM:changePosition(id, position)
	local type, realPos = self:discoverTypeAndPosition(id)
	Log:interceptor("Change " .. tostring(type) .. " interceptor position call")
	if type == "server" then
		local interceptor = self.serverList[realPos]
		if not self.serverList[position] == interceptor then
			self:removeInterceptor(id, type)
			self:addInterceptor(interceptor, id, position, type)
		end
	else
		local interceptor = self.clientList[realPos]
		if not self.list[position] == interceptor then
			self:removeInterceptor(id, type)
			self:addInterceptor(interceptor, id, position, type)
		end
	end
	Log:interceptor("Interceptor position changed.")
end

--
-- Description: Discovers an interceptor position.
--
function IM:getInterceptorPosition(id)
	local type, pos = self:discoverTypeAndPosition(id)
	Log:interceptor("Get " .. type .. " interceptor " .. tostring(id) .. " position call")
	if pos == nil or pos < 1 then
		Log:interceptor("Interceptor not found.")
		error{"IDL:scs/interceptor/InterceptorNotInstalled:1.0"}
	end
	Log:interceptor("Interceptor position is " .. tostring(pos) .. ".")
	return pos
end

--
-- Description: Returns the list of interceptors.
--
function IM:getInterceptorsOrder(type)
	Log:interceptor("Get " .. tostring(type) .. " interceptors order call")
	local result = {}
	if type == "server" then
		if not self:readlockList("server") then
			error{"IDL:scs/interceptor/ListLockFail:1.0"};
		end
		for index, handle in pairs(self.serverList) do
			table.insert(result, handle.instance_id)
		end
		self.serverReadCounter = self.serverReadCounter - 1
	else
		if not self:readlockList("client") then
			error{"IDL:scs/interceptor/ListLockFail:1.0"};
		end
		for index, handle in pairs(self.clientList) do
			table.insert(result, handle.instance_id)
		end
		self.clientReadCounter = self.clientReadCounter - 1
	end
	return result
end


--------------------------------------------------------------------------------
-- Suspension methods
--------------------------------------------------------------------------------

function IM:getBehaviourInString(behaviour)
	if not behaviour then
		behaviour = self.behaviour
	end
	if behaviour == 0 then
		return "normal"
	else
		if behaviour > 0 then
			return "halted"
		else
			return "suspended"
		end
	end
end

--
-- Changes the suspension behaviour. May resume new calls but keep suspended ones if new behaviour is 0.
-- To resume all coroutines resume() must be called.
--
function IM:changeBehaviour(behaviour)
	if not self:lockList("server") then
		error{"IDL:scs/interceptor/ListLockFail:1.0"};
	end
	if not self:lockList("client") then
		self.serverWriteLocked = false
		error{"IDL:scs/interceptor/ListLockFail:1.0"};
	end
	Log:interceptor("Interceptor lists now locked for change in behaviour.")
	self.behaviour = behaviour
  Log:interceptor("ChangeBehaviour call. New behaviour is " .. self:getBehaviourInString(behaviour) .. ".")
	self.serverWriteLocked = false
	self.clientWriteLocked = false
end

--
-- Resumes all components and change behaviour to normal.
--
function IM:resume()
  Log:interceptor("Resume call.")
	if not self:lockList("server") then
		error{"IDL:scs/interceptor/ListLockFail:1.0"};
	end
	if not self:lockList("client") then
		self.serverWriteLocked = false
		error{"IDL:scs/interceptor/ListLockFail:1.0"};
	end
	Log:interceptor("Interceptor lists now locked for change in behaviour.")
	self.behaviour = 0
	self.serverWriteLocked = false
	self.clientWriteLocked = false
	Log:interceptor("Behaviour changed. Resuming coroutines.")
  Log:interceptor("There are ".. #(self.coroutines) .. " waiting.")
	for _, co in ipairs(self.coroutines) do
		oil.tasks:resume(co)
		self.coroutines[_] = nil
	end
--[[
	oil.newthread(
		function (coroutines)
			for _, co in ipairs(coroutines) do
				oil.tasks:resume(co)
			end
		end,
		self.coroutines
	)
--]]
  Log:interceptor("Resume completed.")
end

--
-- Returns the actual behaviour.
--
function IM:getBehaviour()
  Log:interceptor("SuspendContainerInterceptor: GetBehaviour call. Behaviour is " .. self:getBehaviourInString() .. ".")
  return self.behaviour
end

--------------------------------------------------------------------------------
-- Exporting
--------------------------------------------------------------------------------

return IM



--------------------------------------------------------------------------------
-- DISABLED
--------------------------------------------------------------------------------

--[[

--
-- Description: Removes an interceptor from the list by position.
--
function IM:removeInterceptorByPosition(position)
	Log:interceptor("Remove Interceptor by Position call")
	interceptor = interceptor:_narrow()
	if not self:lockList() then
		error{"IDL:scs/interceptor/ListLockFail:1.0"};
	end
	Log:interceptor("Interceptor list now locked.")
	if self.list[position] ~= nil then
		table.remove(self.list, position)
	else
		self.writeLocked = false
		error{"IDL:scs/interceptor/InterceptorNotInstalled:1.0"}
	end
	self.writeLocked = false
	Log:interceptor("Interceptor removed and list unlocked.")
end

--
-- Description: Returns an interceptor by its position.
--
function IM:getInterceptor(position)
	Log:interceptor("Get Interceptor call")
	local interceptor = self.list[position]
	if interceptor == nil then
		Log:interceptor("Invalid position.")
		error{"IDL:scs/interceptor/InterceptorNotInstalled:1.0"}
	end
	Log:interceptor("Interceptor found.")
	return interceptor
end

--]]

