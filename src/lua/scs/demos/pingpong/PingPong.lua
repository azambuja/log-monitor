local oil		= require "oil"
local oo		= require "loop.base"
local comp	= require "loop.component.base"
local port		= require "loop.component.base"
local scs		= require "scs.core.base"
local utils		= require "scs.core.utils"

local orb = oil.orb or oil.init()

local print		= print
local tonumber	= tonumber

orb:loadidlfile("../../../../idl/pingPong.idl")

module "PingPong1.PingPong"

--------------------------------------------------------------------------------
-- PingPong Component
--------------------------------------------------------------------------------

local PingPongBase = oo.class{ utils = false, shutdown = false }

function PingPongBase:__init()
	local inst = oo.rawnew(self, {})
	inst.utils = utils.Utils()
	inst.utils.verbose = true
	return inst
end

--------------------------------------------------------------------------------
-- PingPong Facet
--------------------------------------------------------------------------------

local PingPong = oo.class{ id = 0, stop = false }

function PingPong:ping()
	if self.stop == true then
		self.stop = false
		return 
	end
	local ppFacet = self.context.PingPongRec
	print("PingPong " .. self.id .. " received ping from PingPong " .. ppFacet:getId() .. "! Ponging in 3 seconds...")
	oil.sleep(3)
	oil.newthread(ppFacet.pong, ppFacet)
end

function PingPong:pong()
	if self.stop == true then
		self.stop = false
		return 
	end
	local ppFacet = self.context.PingPongRec
	print("PingPong " .. self.id .. " received pong from PingPong " .. ppFacet:getId() .. "! Pinging in 3 seconds...")
	oil.sleep(3)
	oil.newthread(ppFacet.ping, ppFacet)
end

function PingPong:setId(id)
	self.id = id
end

function PingPong:getId()
	return self.id
end

function PingPong:start()
	print("PingPong " .. self.id .. " received an start call!")
	oil.newthread(self.context.PingPongRec.ping, self.context.PingPongRec)
end

function PingPong:stop()
	self.stop = true
end

--------------------------------------------------------------------------------
-- IComponent Facet
--------------------------------------------------------------------------------

function startup (self)
	self = self.context
	self.utils:verbosePrint("PingPong::IComponent::Startup")
	if self.shutdown == true then
		self.PingPong.stop = true
		self.utils:verbosePrint("PingPong::IComponent::Startup : Restoring facets...")
		scs.restoreFacets(self.context)
	end
	self.utils:verbosePrint("PingPong::IComponent::Startup : Done.")
end

function shutdown (self)
	self = self.context
	self.utils:verbosePrint("PingPong::IComponent::Shutdown")
	self.PingPong.stop = true
	orb:deactivate(self.IComponent)
	orb:deactivate(self.IReceptacles)
	orb:deactivate(self.IMetaInterface)
	orb:deactivate(self.PingPong)
	self.shutdown = true
	self.utils:verbosePrint("PingPong::IComponent::Shutdown : Done.")
end

--------------------------------------------------------------------------------
-- Component Factory
--------------------------------------------------------------------------------

-- LOOP template and factory
local PingPongFactory = comp.Template{
	PingPong				= port.Facet,
	IComponent			= port.Facet,
	IReceptacles   	= port.Facet,
	IMetaInterface 	= port.Facet,
	PingPongRec			= port.Receptacle,
}{  PingPongBase,
	PingPong 				= PingPong,
	IComponent			= scs.Component,
	IReceptacles  	= scs.Receptacles,
	IMetaInterface 	= scs.MetaInterface,
}

local descriptions = {}
descriptions.IComponent			= {}
descriptions.IReceptacles		= {}
descriptions.IMetaInterface	= {}
descriptions.PingPong				= {}
descriptions.PingPongRec		= {}

-- facet descriptions
descriptions.IComponent.name								= "IComponent"
descriptions.IComponent.interface_name			= "IDL:scs/core/IComponent:1.0"
descriptions.IReceptacles.name							= "IReceptacles"
descriptions.IReceptacles.interface_name		= "IDL:scs/core/IReceptacles:1.0"
descriptions.IMetaInterface.name						= "IMetaInterface"
descriptions.IMetaInterface.interface_name	= "IDL:scs/core/IMetaInterface:1.0"
descriptions.PingPong.name									= "PingPong"
descriptions.PingPong.interface_name				= "IDL:scs/demos/pingpong/PingPong:1.0"

-- receptacle descriptions
descriptions.PingPongRec.name						= "PingPongRec"
descriptions.PingPongRec.interface_name	= "IDL:scs/demos/pingpong/PingPong:1.0"
descriptions.PingPongRec.is_multiplex		= false

-- component id
componentId = { name = "PingPong", version = 1.0 }

-- SCS factory
local Factory = oo.class{	pingPongFactory = PingPongFactory, 
													descriptions = descriptions,
													componentId = componentId
						}

-- Exporting
function Factory:create(arg)
	local ppInst = scs.newComponent(self.pingPongFactory, self.descriptions, self.componentId)
	ppInst.IComponent.shutdown = shutdown
	ppInst.PingPong.id = tonumber(arg[1]) or ppInst.PingPong.id
	return ppInst.IComponent
end

return Factory