--
-- SCS - Software Component System
-- OpenbusServerInterceptor.lua
-- Description: OpenbusServerInterceptor component implementation
-- Version: 1.0
--

local oil		= require "oil"
local oo		= require "loop.base"
local comp	= require "loop.component.base"
local port		= require "loop.component.base"
local scs		= require "scs.core.base"
local utils		= require "scs.core.utils"

local PICurrent	= require "openbus.common.PICurrent"
local Log 		= require "openbus.common.Log"

-- If we stored a broker instance previously, use it. If not, use the default broker
local orb = oil.orb or oil.init()

local print	= print
local pairs	= pairs
local ipairs	= ipairs
local tostring 	= tostring
local dofile	= dofile
local string	= string
local assert	= assert
local os		= os
local loadfile	= loadfile

--------------------------------------------------------------------------------
module "scs.interceptor.OpenbusServerInterceptor"
--------------------------------------------------------------------------------

--------------------------------------------------------------------------------
-- OpenbusServerInterceptor Component
--------------------------------------------------------------------------------

local OSIBase = oo.class{
	props = {},
	checkedOperations = {},
	credentialType = false,
	contextID = false,
	picurrent = false,
	accessControlService = false,
}

function OSIBase:__init()
	-- makes sure that table values are recreated for the instance. If not, base class values 
	-- may be modified.
	local inst = oo.rawnew(self, {})
	inst.props = {}
	inst.picurrent = PICurrent()
	return inst
end

--
-- Credential getter
--
function OSIBase:getCredential()
  return self.picurrent:getValue()
end

--
-- Intercepts requests to obtain context information (credential)
--
function OSIBase:receiverequest(request)
  Log:interceptor "OpenbusServerInterceptor: Receive request interception!"
--	local obj = request.servant

  if not (self.checkedOperations.all or
          self.checkedOperations[request.operation]) then
    Log:interceptor ("OpenbusServerInterceptor: Operation "..request.operation.." is not checked")
    return
  end
  Log:interceptor ("OpenbusServerInterceptor: Operation "..request.operation.." is checked")

  local credential = false
  for _, context in ipairs(request.service_context) do
    if context.context_id == self.contextID then
      Log:interceptor "OpenbusServerInterceptor: Credential is present!"
      local decoder = orb:newdecoder(context.context_data)
      credential = decoder:get(self.credentialType)
      Log:interceptor("OpenbusServerInterceptor: Credential: "..credential.identifier..","..credential.entityName)
      break
    end
  end

  if credential then
		-- saves self as active thread so suspension interceptor can liberate isValid call below
		self.container.activeServant[oil.tasks.current] = self
    local success, res = oil.pcall(self.accessControlService.isValid,
                                   self.accessControlService, credential)
		self.container.activeServant[oil.tasks.current] = nil
    if success and res then
      Log:interceptor("OpenbusServerInterceptor: Validated credential for "..request.operation)
      self.picurrent:setValue(credential)
      return
    end
  end

  -- Invalid or no credential
  if credential then
    Log:interceptor("\n *** INVALID CREDENTIAL ***\n")
	  request[1] = orb:newexcept{"CORBA::NO_PERMISSION", minor_code_value = 0}
  else
    Log:interceptor("\n*** NO CREDENTIAL ***\n")
	  request[1] = orb:newexcept{"CORBA::NO_PERMISSION", minor_code_value = 0}
  end
  request.success = false
  request.count = 1
  Log:interceptor "OpenbusServerInterceptor: Receive request interception completed."
end

--
-- Intercepts the response to the request to clean the context
--
function OSIBase:sendreply(reply)
  Log:interceptor "OpenbusServerInterceptor: Send reply interception!"
  reply.service_context = {}
  Log:interceptor "OpenbusServerInterceptor: Send reply interception completed."
end


--------------------------------------------------------------------------------
-- IComponent Facet
--------------------------------------------------------------------------------

--
-- Description: Starts the Server Interceptor.
--
local function startup(self)
end

--
-- Description: Shuts down the Server Interceptor.
--
local function shutdown(self)
	self = self.context
	self.props = nil
	self.checkedOperations = nil
	self.credentialType = nil
	self.contextID = nil
	self.picurrent = nil
	self.accessControlService = nil
	Log:interceptor("Shutting down.")
end

--------------------------------------------------------------------------------
-- Server Interceptor Factory
--------------------------------------------------------------------------------

local serverInterceptorFactory = comp.Template{
	IComponent				= port.Facet,
	IReceptacles			= port.Facet,
	IMetaInterface		= port.Facet,
}{	OSIBase,
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
componentId.name = "OpenbusServerInterceptor"
componentId.version = 1

--------------------------------------------------------------------------------
-- Exporting
--------------------------------------------------------------------------------

local Factory = oo.class{	serverInterceptorFactory = serverInterceptorFactory, 
													descriptions = descriptions,
													componentId = componentId
						}

function Factory:create(args)
  Log:interceptor("Constructing OpenbusServerInterceptor interceptor")
	local SIInst = scs.newComponent(self.serverInterceptorFactory, self.descriptions, self.componentId)
	utils.Utils:readProperties(SIInst.props, "../interceptor/SIConfig.txt")
	-- overriding IComponent methods
	SIInst.IComponent.startup = startup
 	SIInst.IComponent.shutdown = shutdown
	local config = args[1]
  local lir = orb:getLIR()
	SIInst.credentialType = lir:lookup_id(config.credential_type).type
	SIInst.contextID = config.contextID
	SIInst.accessControlService = args[2]
	SIInst.container = args[3]

	SIInst.checkedOperations = {}
  if config.interfaces then
    for _, iconfig in ipairs(config.interfaces) do
      local iface = lir:resolve(iconfig.interface)
      Log:interceptor(true, "OpenbusServerInterceptor: Check interface: " .. iconfig.interface)
      local excluded_ops = iconfig.excluded_ops or {}
      for _, member in ipairs(iface.definitions) do
        local op = member.name
        if member._type == "operation" and not excluded_ops[op] then
          SIInst.checkedOperations[op] = true
          Log:interceptor("OpenbusServerInterceptor: Check operation: " .. op)
        end
      end
      Log:interceptor(false)
    end
  else
    -- If no interface was especified, check all
    SIInst.checkedOperations.all = true
    Log:interceptor("OpenbusServerInterceptor: Check all operations from any interface")
  end

  Log:interceptor("OpenbusServerInterceptor constructed.")
	return SIInst.IComponent
end

return Factory
