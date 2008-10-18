--
-- SCS - Software Component System
-- OpenbusClientInterceptor.lua
-- Description: OpenbusClientInterceptor component implementation
-- Version: 1.0
--

local oil		= require "oil"
local oo		= require "loop.base"
local comp	= require "loop.component.base"
local port		= require "loop.component.base"
local scs		= require "scs.core.base"
local utils		= require "scs.core.utils"

local Log = require "openbus.common.Log"

-- If we stored a broker instance previously, use it. If not, use the default broker
local orb = oil.orb or oil.init()

local print = print

--------------------------------------------------------------------------------
module "scs.interceptor.OpenbusClientInterceptor"
--------------------------------------------------------------------------------

--------------------------------------------------------------------------------
-- OpenbusClientInterceptor Component
--------------------------------------------------------------------------------

local OCIBase = oo.class{
	props = {},
	credentialHolder = false,
	credentialType = false,
	contextID = false,
}

function OCIBase:__init()
	-- makes sure that table values are recreated for the instance. If not, base class values 
	-- may be modified.
	local inst = oo.rawnew(self, {})
	inst.props = {}
	return inst
end

--
-- Intercepts requests from any component to attach context information (credential)
--
function OCIBase:sendrequest(request)
  Log:interceptor("OpenbusClientInterceptor: Send request interception!")
  Log:interceptor("OpenbusClientInterceptor: Client interceptor operation: "..request.operation)

  -- Verifies credential's existence for sending
  if not self.credentialHolder:hasValue() then
    Log:interceptor "OpenbusClientInterceptor: No credential!"
    return
  end
  Log:interceptor("OpenbusClientInterceptor: Has credential!")

  -- Inserts credential at service context
  local encoder = orb:newencoder()
  encoder:put(self.credentialHolder:getValue(), 
              self.credentialType)
  request.service_context =  {
    { context_id = self.contextID, context_data = encoder:getdata() }
  }
  Log:interceptor("OpenbusClientInterceptor: Credential inserted.")
  Log:interceptor("OpenbusClientInterceptor: Send request interception completed.")
end

--
-- Intercepts the received reply
--
function OCIBase:receivereply(reply)
  Log:interceptor("OpenbusClientInterceptor: Receive reply interception!")
  Log:interceptor("OpenbusClientInterceptor: Receive reply interception completed.")
end


--------------------------------------------------------------------------------
-- IComponent Facet
--------------------------------------------------------------------------------

--
-- Description: Starts the Client Interceptor.
--
local function startup(self)
end

--
-- Description: Shuts down the Client Interceptor.
--
local function shutdown(self)
	self = self.context
	self.props = nil
	self.credentialHolder = nil
	self.credentialType = nil
	self.contextID = nil
	Log:interceptor("Shutting down.")
end

--------------------------------------------------------------------------------
-- Client Interceptor Factory
--------------------------------------------------------------------------------

local clientInterceptorFactory = comp.Template{
	IComponent				= port.Facet,
	IReceptacles			= port.Facet,
	IMetaInterface		= port.Facet,
}{	OCIBase,
	IComponent				= scs.Component,
	IReceptacles			= scs.Receptacles,
	IMetaInterface		= scs.MetaInterface,
}

local descriptions = {}
descriptions.IComponent					= {}
descriptions.IReceptacles				= {}
descriptions.IMetaInterface			= {}

-- facet descriptions
descriptions.IComponent.name									= "IComponent"
descriptions.IComponent.interface_name				= "IDL:scs/core/IComponent:1.0"

descriptions.IReceptacles.name								= "IReceptacles"
descriptions.IReceptacles.interface_name			= "IDL:scs/core/IReceptacles:1.0"

descriptions.IMetaInterface.name							= "IMetaInterface"
descriptions.IMetaInterface.interface_name		= "IDL:scs/core/IMetaInterface:1.0"

-- component id
local componentId = {}
componentId.name = "OpenbusClientInterceptor"
componentId.version = 1

--------------------------------------------------------------------------------
-- Exporting
--------------------------------------------------------------------------------

local Factory = oo.class{	clientInterceptorFactory = clientInterceptorFactory, 
													descriptions = descriptions,
													componentId = componentId
						}

function Factory:create(args)
  Log:interceptor("Constructing OpenbusClientInterceptor interceptor")
	local CIInst = scs.newComponent(self.clientInterceptorFactory, self.descriptions, self.componentId)
	utils.Utils:readProperties(CIInst.props, "../interceptor/CIConfig.txt")
	-- overriding IComponent methods
	CIInst.IComponent.startup = startup
	CIInst.IComponent.shutdown = shutdown
	local config = args[1]
  local lir = orb:getLIR()
	CIInst.credentialType = lir:lookup_id(config.credential_type).type
	CIInst.contextID = config.contextID
	CIInst.credentialHolder = args[2]
  Log:interceptor("OpenbusClientInterceptor constructed.")
	return CIInst.IComponent
end

return Factory
