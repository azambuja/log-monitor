--
-- SCS - Software Component System
-- ExecutionNode.lua
-- Description: Execution Node component implementation
-- Version: 1.0
--

local oil		= require "oil"
local oo		= require "loop.base"
local comp	= require "loop.component.base"
local port		= require "loop.component.base"
local utils		= require "scs.core.utils"
local scsprops	= require "scs.auxiliar.componentproperties"

-- loading properties to check openbus and OiL requirements
local props = {}
utils.Utils:readProperties(props, "Properties.txt")

-- OpenBus configuration
if not props.use_openbus then
	props.use_openbus = { name = "use_openbus", value = "false", read_only = true}
end

local	ClientInterceptor = false
local	ServerInterceptor = false
local	CredentialHolder  = false
local	ServiceConnectionManager = false
local	Log = false

if props.use_openbus.value == "true" then
	ClientInterceptor = require "openbus.common.ClientInterceptor"
	ServerInterceptor = require "openbus.common.ServerInterceptor"
	CredentialHolder  = require "openbus.common.CredentialHolder"
	ServiceConnectionManager = require "openbus.common.ServiceConnectionManager"
	Log = require "openbus.common.Log"
	Log:level(3)
end

-- OiL configuration
local oilconf = { tcpoptions = {reuseaddr = true} }
if props.host then
	oilconf.host = props.host.value
end
if props.port then
	oilconf.port = tonumber(props.port.value)
end
if props.interception then
	oilconf.flavor = props.interception.value
end
if props.oilverbose then
	oil.verbose:level(tonumber(props.oilverbose.value))
end

--oil.init({host = "localhost", port = 20202})
local orb = oil.init(oilconf)
oil.orb = orb

-- now we can load up modules that require the same orb
local scs		= require "scs.core.base"

-- load needed idls
orb:loadidlfile("../../../../idl/deployment.idl")

-- requirements
local loadfile  = loadfile
local _G	= _G
local os	= os
local io	= io
local tonumber  = tonumber
local tostring  = tostring
local string    = string
local error	= error
local assert	= assert
local print = print
local pairs = pairs
local ipairs = ipairs

local winPlat = { WIN = true, WINDOWS = true, WINNT = true, WINDOWSNT = true, WINDOWS_NT = true }
local linuxPlat = { LINUX = true, UNIX = true }

--------------------------------------------------------------------------------

module "scs.execution_node.ExecutionNode"

--------------------------------------------------------------------------------

--------------------------------------------------------------------------------
-- ExecutionNode Component
--------------------------------------------------------------------------------

local ENBase = oo.class{
	containerDescriptions = {},
	props = {},
	started = false,
	name = "",
	utils = false,
}

function ENBase:__init()
	-- makes sure that table values are recreated for the instance. If not, base class values
	-- may be modified.
	local inst = oo.rawnew(self, {})
	inst.utils = utils.Utils()
	inst.utils.verbose = true
	inst.utils.fileVerbose = true
	inst.utils.fileName = "execution_node"
	inst.containerDescriptions = {}
	inst.props = {}
	return inst
end

--------------------------------------------------------------------------------
-- ExecutionNode Facet
--------------------------------------------------------------------------------

local ExecNode = oo.class{}

--
-- Description: Informs the name of this Execution Node.
-- Return value: The name.
--
function ExecNode:getName()
	self = self.context
	self.utils:verbosePrint("ExecutionNode::ExecutionNode::getName")
	self.utils:verbosePrint("ExecutionNode::ExecutionNode::getName : Finished.")
	return self.props.host.value
end

--
-- Description: Starts a new container process.
-- Parameter name: Container's name.
-- Return Value: New container's IComponent reference.
-- Throws: IDL:ContainerAlreadyExists, IDL:InvalidProperty exceptions
--
function ExecNode:startContainer(name, props)
	self = self.context
	self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer")
	self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : Name is " .. name ..
							".")

	self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : Testing if container" ..
							" is running...")
	-- tests if the container is already running
	if self.containerDescriptions[name] or not name then
		self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : True. This " ..
								"container already exists, launching exception.")
		error{"IDL:scs/execution_node/ContainerAlreadyExists:1.0"}
	end

	-- reads container properties
	local machine = false
	local language = false
	local classpath = false
	for _, prop in ipairs(props) do
		if prop.name == "machine" then
			machine = string.lower(prop.value)
		end
		if prop.name == "language" then
			language = string.lower(prop.value)
		end
		if prop.name == "classpath" then
			classpath = string.lower(prop.value)
		end
	end
	-- tests the existence of the supplied machine
	if machine then
		local found = false
		for w in string.gmatch(self.props.machines.value, "%a+") do
			if w == machine then
				found = true
				break
			end
		end
		if not found then
			error{"IDL:scs/execution_node/RequirementNotMet:1.0", "The required machine, " .. machine ..
				", is not available."
			}
		end
	end

	-- create a new container process
	local platform = string.upper(self.props.platform.value)
	self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : False.")
	self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : Creating new " ..
							"container process and waiting for it to register...")
	if not language or language == "lua" then
		if not machine then machine = self.props.lua_name.value end
		local luaExecStr = machine .. " ../container/ComponentContainer.lua " .. name
		if self.props.use_openbus.value == "true" then
			-- use_openbus option in scs-lua is -o
			luaExecStr = luaExecStr .. " -O"
			-- TODO: add option for openbus with scs-java
		end
		self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : Container type " ..
								"is Lua and platform is " .. platform .. ".")
		-- load init file upon execution
		luaExecStr = luaExecStr .. " -L ComponentContainerInit.lua"
		if winPlat[platform] then
			self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : Creating new" ..
									" windows process...")
			os.execute("start " .. luaExecStr)
		elseif linuxPlat[platform] then
			self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : Creating new " ..
									"unix process in background...")
			os.execute(luaExecStr .. " &")
		else
			self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : Platform " ..
									platform .. " is not supported!")
			error{"IDL:scs/execution_node/LoadFailure:1.0"}
		end
	elseif language == "java" then
		if not machine then machine = "java" end
		if not classpath then error{"IDL:scs/execution_node/InvalidProperty:1.0", "Java classpath not defined."} end
		local javaExecStr = machine .. " -classpath " .. classpath .. 
			" scs.container.servant.ContainerApp ..//..//..//..//scripts//execute//scs.properties " .. 
			orb:tostring(enInst.ContainerManager) .. " null " .. name .. " >STDOUT 2>STDERR"
		self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : Container type " ..
								"is Java and platform is " .. platform .. ".")
		-- TODO: add option for loading init file in scs-java
		if winPlat[platform] then
			self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : Creating new " ..
									" windows process...")
			os.execute("start " .. javaExecStr)
		elseif linuxPlat[platform] then
			self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : Creating new " ..
									"unix process in background...")
			os.execute(javaExecStr .. " &")
		else
			self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : Platform " ..
									platform .. " is not supported!")
			error{"IDL:scs/execution_node/LoadFailure:1.0"}
		end
	else
		self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : This Execution " ..
								"Node does not support " .. language .. " containers yet!")
				error{"IDL:scs/execution_node/LoadFailure:1.0"}
	end

	local numWaits = 1
	local maxWaits = tonumber(self.props.timeout.value)
	while numWaits <= maxWaits do
		if not self.containerDescriptions[name] then
			self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : Waiting 1 " ..
									"second for container " .. name .. " to register. Times: " ..
									numWaits .. ".")
			oil.sleep(1)
			numWaits = numWaits + 1
		else
			break
		end
	end

	if not self.containerDescriptions[name] then
		self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : Could not start " ..
								"container.")
		self.containerDescriptions[name] = nil
		error("IDL:scs/execution_node/TimeOutException:1.0")
-- 		return nil
	end

--[[
	-- call container's startup
	oil.newthread(
		function (self)
			self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : Starting container...")
			local status, err = oil.pcall(self.containerDescriptions[name].container.startup,
										self.containerDescriptions[name].container)
			if not status then
				self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : Could not start " ..
											"container.\nError: " .. err)
				self.containerDescriptions[name] = nil
				error{"IDL:scs/execution_node/LoadFailure:1.0"}
--       return nil
			end
		end,
		self
	)
--]]
--[[
	-- install Openbus interceptors if needed
	if self.use_openbus.value == "true" then
		oil.newthread(
			function (self)
				-- obtain component interception facet
				self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : Installing interceptors...")
				local status, CI = oil.pcall(self.containerDescriptions[name].container.getFacetByName,
											self.containerDescriptions[name].container, "ComponentInterception")
				if not status then
					self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : Could not obtain " ..
											"component interception facet.\nError: " .. CI)
					return
				end
				CI = orb:narrow(CI)
				-- load server interceptor
				local status, err = oil.pcall(CI.loadInterceptor, CI,
											{name = "OpenbusServerInterceptor", version = 1.0}, {}, 1)
				if not status then
					self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : Could not start " ..
											"server interceptor at container.\nError: " .. err)
				end
				-- load client interceptor
				status, err = oil.pcall(CI.loadInterceptor, CI,
											{name = "OpenbusClientInterceptor", version = 1.0}, {}, 2)
				if not status then
					self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : Could not start " ..
											"client interceptor at container.\nError: " .. err)
				end
			end,
			self
		)
	end
--]]

	self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : Container created and registered!")
	self.utils:verbosePrint("ExecutionNode::ExecutionNode::StartContainer : Finished.")
	return self.containerDescriptions[name].container
end

--
-- Description: Stops a container process.
-- Parameter name: Container's name.
-- Throws: IDL:InvalidName exception
--
function ExecNode:stopContainer(name)
	self = self.context
	self.utils:verbosePrint("ExecutionNode::ExecutionNode::StopContainer")

	if not self.containerDescriptions[name] then
		self.utils:verbosePrint("ExecutionNode::ExecutionNode::StopContainer : Container " .. name ..
							" does not exist.")
		error{"IDL:scs/execution_node/InvalidName:1.0"}
	end

	-- stops container
	local status, err = oil.pcall(self.containerDescriptions[name].container.shutdown, self.containerDescriptions[name])
	if not status then
		self.utils:verbosePrint("ExecutionNode::ExecutionNode::StopContainer : Error stopping container " .. self.name .. ": " .. err)
		return
	end

	-- unregister
	self.ContainerManager:unregisterContainer(name)
	self.utils:verbosePrint("ExecutionNode::ExecutionNode::StopContainer : Stopped and unregistered  container " .. self.name .. ".")
	self.utils:verbosePrint("ExecutionNode::ExecutionNode::StopContainer : Finished.")
end

--
-- Description: Returns a specific container.
-- Parameter name: Container's name.
-- Return Value: Container's IComponent.
-- Throws: IDL:InvalidParameter exception
--
function ExecNode:getContainer(name)
	self = self.context
	self.utils:verbosePrint("ExecutionNode::ExecutionNode::GetContainer")
	if self.containerDescriptions[name] then
		self.utils:verbosePrint("ExecutionNode::ExecutionNode::GetContainer : Returning container " ..
								name .. ". Finished.")
		return self.containerDescriptions[name].container
	end
	self.utils:verbosePrint("ExecutionNode::ExecutionNode::GetContainer : Container " .. name ..
							" does not exist. Returning nil.")
end

--
-- Description: Informs all created containers.
-- Return Value: All container descriptions.
--
function ExecNode:getContainers()
	self = self.context
	self.utils:verbosePrint("ExecutionNode::ExecutionNode::GetContainers")
	local outputArray = self.utils:convertToArray(self.containerDescriptions)
	self.utils:verbosePrint("ExecutionNode::ExecutionNode::GetContainers : Finished.")
	return outputArray
end

--------------------------------------------------------------------------------
-- ContainerManager Facet
--------------------------------------------------------------------------------

local CtnManager = oo.class{}

--
-- Description: Registers a container.
-- Parameter name: Container's name.
-- Parameter component: Container's IComponent object.
-- Throws: IDL:ContainerAlreadyExists, IDL:InvalidContainer exceptions
--
function CtnManager:registerContainer(name, ctr)
	self = self.context
	self.utils:verbosePrint("ExecutionNode::ContainerManager::RegisterContainer")
	if not ctr or not ctr:_is_a("IDL:scs/core/IComponent:1.0") then error{ "IDL:InvalidContainer" } end
	-- registers the container
	self.utils:verbosePrint("ExecutionNode::ContainerManager::RegisterContainer : Testing if " ..
							"name is nil or container already exists...")
	-- tests if the container already exists
	if self.containerDescriptions[name] or not name then
		self.utils:verbosePrint("ExecutionNode::ContainerManager::RegisterContainer : True, " ..
								"launching exception.")
		error{ "IDL:scs/execution_node/ContainerAlreadyExists:1.0" }
	end
	self.utils:verbosePrint("ExecutionNode::ContainerManager::RegisterContainer : False. " ..
							"Proceeding to register container...")

	self.containerDescriptions[name] = {container_name = name,
										container = ctr,
										execution_node = self.IComponent}
	f = io.open("containers.ior", "a")
	if f then
		f:write(name .. "\n")
		f:write(orb:tostring(ctr) .. "\n")
		f:close()
	end
	self.utils:verbosePrint("ExecutionNode::ContainerManager::RegisterContainer : Container " ..
							name .. " registered and persisted. Finished.")
end

--
-- Description: Unregisters a container.
-- Parameter name: Container's name.
-- Throws: IDL:InvalidParameter, IDL:InvalidName exceptions
--
function CtnManager:unregisterContainer(name)
	self = self.context
	self.utils:verbosePrint("ExecutionNode::ContainerManager::UnregisterContainer")
	assert(self.containerDescriptions[name], "IDL:InvalidName")
	-- removes the container
	self.utils:verbosePrint("ExecutionNode::ContainerManager::UnregisterContainer : Removing " ..
							"container " .. name .. "...")
	self.containerDescriptions[name].execution_node = nil
	self.containerDescriptions[name] = nil
	self.utils:verbosePrint("ExecutionNode::ContainerManager::UnregisterContainer : Finished.")
end

--------------------------------------------------------------------------------
-- IComponent Facet
--------------------------------------------------------------------------------

--
-- Description: Starts the Execution Node.
--
local function startup(self)
	self = self.context
	self.utils:verbosePrint("ExecutionNode::IComponent::Startup")
	if not self.started then
		if self.props.initfile then
			self.utils:verbosePrint("ExecutionNode::IComponent::Startup : Executing init file " ..
									self.props.initfile.value .. "...")
			local func, err = loadfile(self.props.initfile.value)
			if not func then
				self.utils:verbosePrint("Error loading init file! Error: " .. err)
				error(err)
			end
			oil.newthread(func)
		end
		-- recover containers
		local containers = {}
		local f = io.open("containers.ior", "r")
		if f then
			while true do
				local name = f:read("*l")
				if name == nil then
					break
				end
				local ior = f:read("*l")
				self.utils:verbosePrint("ExecutionNode::IComponent::Startup : Recovering container...")
				local status, container = oil.pcall(orb.newproxy, orb, ior)
				if status then
					container = orb:narrow(container, "IDL:scs/core/IComponent:1.0")
					if container then
						containers[name] = {container = container}
					end
				end
			end
			f:close()
		end
		-- erase contents of file
		local f = io.open("containers.ior", "w")
		if f then
			f:close()
		end
		-- re-register them
		for name, holder in pairs(containers) do
			local status, err = oil.pcall(self.ContainerManager.registerContainer, self.ContainerManager, name, holder.container)
			if not status then
				self.utils:verbosePrint("ExecutionNode::IComponent::Startup : Error registering recovered container: " .. err)
			end
		end

		self.started = true
		self.utils:verbosePrint("ExecutionNode::IComponent::Startup : Finished.")
	else
		self.utils:verbosePrint("ExecutionNode::IComponent::Startup : This execution node is " ..
								"already started. Done nothing.")
	end
end

--
-- Description: Shuts down the Execution Node.
--
local function shutdown(self)
	self = self.context
	self.utils:verbosePrint("ExecutionNode::IComponent::Shutdown")
	self.utils:verbosePrint("ExecutionNode::IComponent::Shutdown : Trying to shutdown all containers...")
	if self.defaultContainer then
		oil.newthread(self.defaultContainer.shutdown, self.defaultContainer)
	end
	if not self.props.restart or self.props.restart.value == false then
		for index, desc in pairs(self.containerDescriptions) do
			oil.newthread(desc.container.shutdown, desc.container)
		end

		local maxWait = 20
		if self.props.timeout then
			local maxWait = tonumber(self.props.timeout.value)
		end
		local numWaits = 1
		self.utils:verbosePrint("ExecutionNode::IComponent::Shutdown : Waiting "  .. maxWait ..
								" seconds for containers...")
		while numWaits <= maxWait do
			oil.sleep(1)
			self.utils:verbosePrint("ExecutionNode::IComponent::Shutdown : Waited " .. numWaits ..
									" second(s)...")
			numWaits = numWaits + 1
		end
	end

	orb:deactivate(self.IComponent)
	orb:deactivate(self.IReceptacles)
	orb:deactivate(self.IMetaInterface)
	orb:deactivate(self.ComponentProperties)
	orb:deactivate(self.ExecutionNode)
	orb:deactivate(self.ContainerManager)
	orb:shutdown()
	self.utils:verbosePrint("ExecutionNode::IComponent::Shutdown : Exiting.")
	if self.props.restart and self.props.restart.value == true then
		os.execute(self.props.lua_name.value .. " ExecutionNode.lua")
	end
	os.exit()
end

--------------------------------------------------------------------------------
-- ExecutionNode Factory
--------------------------------------------------------------------------------

local executionNodeFactory = comp.Template{
	IComponent			= port.Facet,
	IReceptacles   			= port.Facet,
	IMetaInterface 		= port.Facet,
	ComponentProperties	= port.Facet,
	ExecutionNode 		= port.Facet,
	ContainerManager		= port.Facet,
}{	ENBase,
	IComponent			= scs.Component,
	IReceptacles   			= scs.Receptacles,
	IMetaInterface 		= scs.MetaInterface,
	ComponentProperties	= scsprops.CpnProperties,
	ExecutionNode     		= ExecNode,
	ContainerManager		= CtnManager,
}

local descriptions = {}
descriptions.IComponent 		= {}
descriptions.IReceptacles		= {}
descriptions.IMetaInterface		= {}
descriptions.ComponentProperties	= {}
descriptions.ExecutionNode		= {}
descriptions.ContainerManager	= {}

descriptions.IComponent.name 					= "IComponent"
descriptions.IComponent.interface_name			= "IDL:scs/core/IComponent:1.0"

descriptions.IReceptacles.name					= "IReceptacles"
descriptions.IReceptacles.interface_name			= "IDL:scs/core/IReceptacles:1.0"

descriptions.IMetaInterface.name 					= "IMetaInterface"
descriptions.IMetaInterface.interface_name			= "IDL:scs/core/IMetaInterface:1.0"

descriptions.ComponentProperties.name 			= "ComponentProperties"
descriptions.ComponentProperties.interface_name	= "IDL:scs/auxiliar/ComponentProperties:1.0"

descriptions.ExecutionNode.name 					= "ExecutionNode"
descriptions.ExecutionNode.interface_name			= "IDL:scs/execution_node/ExecutionNode:1.0"

descriptions.ContainerManager.name 				= "ContainerManager"
descriptions.ContainerManager.interface_name		= "IDL:scs/execution_node/ContainerManager:1.0"

-- component id
local componentId = {}
componentId.name = "ExecutionNode"
componentId.version = 1

--------------------------------------------------------------------------------
-- Exporting
--------------------------------------------------------------------------------

oil.main(function()
	-- starts to wait for remote calls
  	oil.newthread(orb.run, orb)
	enInst = scs.newComponent(executionNodeFactory, descriptions, componentId)

	-- overriding IComponent methods
	enInst.IComponent.startup = startup
	enInst.IComponent.shutdown = shutdown

	enInst.utils.verbose = true
	enInst.utils.file_verbose = false

	enInst.props = props
	-- must be global so init file can access it
	_G.enInst = enInst

	if not enInst.props.lua_name then
		enInst.utils:verbosePrint("Properties file must contain lua_name property!")
		enInst.IComponent:shutdown()
	end
	if not enInst.props.platform then
		enInst.utils:verbosePrint("Properties file must contain platform property!")
		enInst.IComponent:shutdown()
	end

	if enInst.props.use_openbus.value == "true" then
		-- OPENBUS SETUP
		enInst.props.accessControlServerHost = {name = "accessControlServerHost", read_only = true}
		enInst.props.accessControlServerHost.value = enInst.props.accessControlServerHostName.value .. ":" ..
				enInst.props.accessControlServerHostPort.value
		local credentialHolder = CredentialHolder()
		enInst.connectionManager =  ServiceConnectionManager(
				enInst.props.accessControlServerHost.value, credentialHolder,
				enInst.props.privateKeyFile.value,
				enInst.props.accessControlServiceCertificateFile.value)

		-- Loads access control interface
		local idldir = os.getenv("CORBA_IDL_DIR")
		local idlfile = idldir .. "/access_control_service.idl"
		orb:loadidlfile (idlfile)

		-- Obtains a reference to the access control service
		enInst.accessControlService = enInst.connectionManager:getAccessControlService()
		if enInst.accessControlService == nil then
			enInst.utils:verbosePrint("Could not resolve access control service!")
			enInst.IComponent:shutdown()
		end

		-- installs client interceptor (intercepts outgoing calls)
		local CONF_DIR = os.getenv("CONF_DIR")
		local interceptorsConfig = assert(loadfile(CONF_DIR.."/advanced/InterceptorsConfiguration.lua"))()

		-- installs client interceptor (intercepts outgoing calls)
		orb:setclientinterceptor(ClientInterceptor(interceptorsConfig, credentialHolder))

			-- Adds info about interfaces to be checked
		interceptorsConfig.interfaces = {
			{
				interface = "IDL:scs/execution_node/ExecutionNode:1.0",
				excluded_ops = { }
			},
			{
				interface = "IDL:scs/execution_node/ContainerManager:1.0",
				excluded_ops = { }
			}
		}

		-- installs server interceptor (intercepts incoming call on specified facets)
		enInst.serverInterceptor = ServerInterceptor(interceptorsConfig, enInst.accessControlService)
		orb:setserverinterceptor(enInst.serverInterceptor)

		-- autenticates EN, connecting it to the bus
		local success = enInst.connectionManager:connect("ExecutionNode", nil)
		if not success then
			enInst.utils:verbosePrint("Could not connect to bus!")
			enInst.IComponent:shutdown()
		end

		-- offers service at registryService
		local idlfile = idldir .. "/registry_service.idl"
		orb:loadidlfile (idlfile)
		enInst.registryService = enInst.accessControlService:getRegistryService()
		local success, registryIdentifier = enInst.registryService:register({type = "ExecutionNode",
				description = "Execution node from machine " .. enInst.props.host.value,
				properties = {host = enInst.props.host.value}, member = enInst.IComponent })
		if not success then
			enInst.utils:verbosePrint("Could not register at registry service!")
			enInst.IComponent:shutdown()
		end
		enInst.props.registryIdentifier = {name = "", read_only = true}
		enInst.props.registryIdentifier.value = registryIdentifier
		-- END OF OPENBUS SETUP
	end

	oil.writeto("execution_node.ior", orb:tostring(enInst.IComponent))
end)
