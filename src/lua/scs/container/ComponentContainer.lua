--
-- SCS - Software Component System
-- ComponentContainer.lua
-- Description: Component Container component implementation
-- Version: 1.0
--

local oil = require "oil"

-- oil.ValueEncoder.objects = nil

local oo		= require "loop.base"
local comp	= require "loop.component.base"
local port		= require "loop.component.base"
local utils		= require "scs.core.utils"
local help		= require "scs.auxiliar.componenthelp"
local scsprops	= require "scs.auxiliar.componentproperties"

-- loading properties to check openbus and OiL requirements
local props = {}
utils.Utils:readProperties(props, "../container/Properties.txt")

-- argument treatment for openbus
for index, arg in ipairs(arg) do
	if string.upper(tostring(arg)) == "-O" then
		if not props.use_openbus then
 			props.use_openbus = { name = "use_openbus", read_only = true}
		end
		props.use_openbus.value = "true"
	end
end

local CredentialHolder  = false
local ServiceConnectionManager = false
local Log = false

-- OpenBus configuration
if not props.use_openbus then
	props.use_openbus = { name = "use_openbus", value = "false", read_only = true}
end
if props.use_openbus.value == "true" then
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
-- oil.tasks.verbose:flag("threads", true)
local orb = oil.init(oilconf)
oil.orb = orb

-- DISABLE OIL OPTIMIZATION FOR COLLOCATION
-- get IOR profilers connected to the multiple receptacle
local connections = {}
for conn_id in orb.ValueEncoder.profiler:__all() do
       connections[#connections+1] = conn_id
end
-- remove the connections so local references are not recognized
for _, conn_id in ipairs(connections) do
       orb.ValueEncoder.profiler:__unbind(conn_id)
end
-- END DISABLE OIL OPTIMIZATION FOR COLLOCATION


-- now we can load up modules that require the same orb
local scs		= require "scs.core.base"
local IM		= require "scs.interceptor.InterceptorsManager"

-- load needed idls
orb:loadidlfile("../../../../idl/deployment.idl")
orb:loadidlfile("../../../../idl/repository.idl")

local _G 	 			= _G
local assert 		= assert
local error			= error
local io				= io
local ipairs		= ipairs
local loadfile	= loadfile
local os				= os
local pairs	 		= pairs
local print  		= print
local require		= require
local string		= string
local table     = table
local tonumber  = tonumber
local tostring  = tostring
local next			= next
local getmetatable = getmetatable

local winPlat = { WIN = true, WINDOWS = true, WINNT = true, WINDOWSNT = true, WINDOWS_NT = true }
local linuxPlat = { LINUX = true, UNIX = true }

--------------------------------------------------------------------------------

module "scs.container.ComponentContainer"

--------------------------------------------------------------------------------

--------------------------------------------------------------------------------
-- ComponentContainer Component
--------------------------------------------------------------------------------

local CtnBase = oo.class{
	-- internal cache, keeps the component descriptions that are in disk. Indexed by name..version
	componentFullDescriptions = {},
	componentHandles = {},
	-- cache's componentIds indexer
	componentIds = {},
	componentConnections = {},
	props = {},
	containerName = "",
	maxInstanceId = 0,
	numComponents = 0,
	started = false,
	utils = false,
	SCIClient = false,
	SCIServer = false,
	activeServant = {},
}

function CtnBase:__init()
	-- makes sure that table values are recreated for the instance. If not, base class values 
	-- may be modified.
	local inst = oo.rawnew(self, {})
	inst.utils = utils.Utils()
	inst.utils.verbose = true
	inst.utils.fileVerbose = true
	inst.utils.fileName = "container"
	inst.componentFullDescriptions = {}
	inst.componentHandles = {}
	inst.componentIds = {}
	inst.componentConnections = {}
	inst.props = {}
	inst.IM = IM(inst, 0)
	inst.activeServant = {}
	return inst
end

-- this is a private function, not available in any facet
function CtnBase:checkReposForComponent(componentId)
	self.utils:verbosePrint("ComponentContainer::ComponentLoader::checkReposForComponent")
	self.utils:verbosePrint("ComponentContainer::ComponentLoader::checkReposForComponent : Trying" ..
							" to retrieve it from a known repository...")
	local nameVersion = componentId.name .. componentId.version
	local octetSeq = false
	local tempDesc = false
	local tempHelp = false
	-- tries to get octetSeq and componentDescription from a known repository
	local finished = false
	local repositoryRef = -1
	for index, connection in pairs(self._receptacleDescs.ComponentRepository.connections) do
		self.utils:verbosePrint("ComponentContainer::ComponentLoader::checkReposForComponent : " ..
								"Trying repository with connectionId " .. connection.id .. ".")
 		local repositoryRef = connection.objref
		-- asking for octetSeq
		local status, oct = oil.pcall(  repositoryRef.getComponentFile, 
										repositoryRef, 
										componentId)
		octetSeq = oct
		if not status then 
			self.utils:verbosePrint("ComponentContainer::ComponentLoader::checkReposForComponent" ..
									": Unable to retrieve " .. componentId.name .. 
									"'s octet sequence.\nError: " .. oct)
		else
			finished = true
		end
		-- now it may have finished
		if finished then
			-- Asking for description in the same repository
			self.utils:verbosePrint("ComponentContainer::ComponentLoader::checkReposForComponent" ..
									" : Now getting component's description...")
			local status, desc = oil.pcall( repositoryRef.getComponentDescription, 
											repositoryRef, 
											componentId)
			if not status then 
				self.utils:verbosePrint("ComponentContainer::ComponentLoader::checkReposForComponent" ..
										" : Unable to retrieve " .. componentId.name .. 
										"'s component description, will try other repositories.\nError: " ..
										desc)
				for index, connection in pairs(self._receptacleDescs.ComponentRepository.connections) do
					-- Asking for description in all repositories
					local status, desc = oil.pcall( repositoryRef.getComponentDescription, 
													repositoryRef, 
													componentId)
					if not status then 
						self.utils:verbosePrint("ComponentContainer::ComponentLoader::checkReposForComponent" ..
												" : Unable to retrieve " .. componentId.name .. 
												"'s component description.\nError: " .. desc)
					else
						self.utils:verbosePrint("ComponentContainer::ComponentLoader::checkReposForComponent" ..
												" : Found " .. componentId.name .. "'s component description.")
						tempDesc = desc
						break
					end
				end
			else
				self.utils:verbosePrint("ComponentContainer::ComponentLoader::checkReposForComponent" ..
										" : Found " .. componentId.name .. "'s component description.")
				tempDesc = desc
			end
			break
		end -- if finished
	end
	if finished then
		-- Asking for help in connected facets
		self.utils:verbosePrint("ComponentContainer::ComponentLoader::checkReposForComponent :" ..
								" Now getting component's help...")
		for index, connection in pairs(self._receptacleDescs.ComponentHelpRecept.connections) do
			self.utils:verbosePrint("ComponentContainer::ComponentLoader::checkReposForComponent : " ..
									"Trying help with connectionId " .. connection.id .. ".")
			local helpRef = connection.objref
			local status, help = oil.pcall(helpRef.getHelpInfo, helpRef, componentId)
			if not status then 
				self.utils:verbosePrint("ComponentContainer::ComponentLoader::checkReposForComponent" ..
										" : Unable to retrieve " .. componentId.name .. 
										"'s help info, will keep trying other sources if available.\nError: " .. help)
			else
				self.utils:verbosePrint("ComponentContainer::ComponentLoader::checkReposForComponent" ..
										" : Found " .. componentId.name .. "'s help info.")
				tempHelp = help
				break
			end
		end

		self.utils:verbosePrint("ComponentContainer::ComponentLoader::checkReposForComponent : " ..
								"Octet size is " .. string.len(octetSeq) .. ".")
		assert(octetSeq, "IDL:scs/container/ComponentNotFound:1.0")
		assert(tempDesc, "IDL:scs/container/ComponentNotFound:1.0")
--		assert(tempDesc.extension == "lua", "IDL:scs/container/LoadFailure:1.0")

		-- saves component at the HD to be executed
		self.utils:verbosePrint("ComponentContainer::ComponentLoader::checkReposForComponent : Saving" ..
								" component in local disk cache...")
		-- checks if the file is already in the HD. 
		-- If yes, we should not write it over because there may be already some component process 
		-- running on this machine.
		local f = io.open("../container/" ..
							nameVersion .. "/" ..
							componentId.name ..
							"." ..
							tempDesc.extension, "rb")
		if not f then
			os.execute("mkdir \"../container/" .. nameVersion .. "\"")
			f = assert(io.open("../container/" .. nameVersion .. "/" .. componentId.name .. "." ..
								tempDesc.extension, "wb"), "IDL:scs/container/LoadFailure:1.0")
			f:write(octetSeq)
		end
		f:close()
		-- saves in cache
		self.componentFullDescriptions[nameVersion] = {}
		self.componentFullDescriptions[nameVersion].description = tempDesc
		if tempHelp ~= false then
			self.componentFullDescriptions[nameVersion].help = tempHelp
		end
	end
	self.utils:verbosePrint("ComponentContainer::ComponentLoader::checkReposForComponent : Finished.")
end

--------------------------------------------------------------------------------
-- ComponentLoader Facet
--------------------------------------------------------------------------------

local CpnLoader = oo.class{}

function CpnLoader:__init()
	return oo.rawnew(self, {})
end

--
-- Description: Informs the available components.
-- Return Value: The ComponentIds.
--
function CpnLoader:getInstalledComponents()
	self = self.context
	self.utils:verbosePrint("ComponentContainer::ComponentLoader::GetInstalledComponents")
	self.utils:verbosePrint("ComponentContainer::ComponentLoader::GetInstalledComponents : Finished.")
	return self.componentIds
end
   
--
-- Description: Loads a LUA component.
-- Parameter componentId: Component's identifier.
-- Parameter args: Arguments to be passed.
-- Return Value: Component's handle.
-- Throws: IDL:ComponentNotFound, IDL:ComponentAlreadyLoaded, IDL:LoadFailure exceptions
--
function CpnLoader:load(componentId, arg)
	self = self.context
	self.utils:verbosePrint("ComponentContainer::ComponentLoader::Load")
	local firstRepository = false
	local nameVersion = componentId.name .. componentId.version
	
	-- tests if the component is in the local cache. If we have the description, the file is in the 
	-- hd. If not, we must find/retrieve it.
	self.utils:verbosePrint("ComponentContainer::ComponentLoader::Load : Testing if the component " ..
							nameVersion .. " is in the local disk cache...")
	if not self.componentFullDescriptions[nameVersion] then
		if self._receptacleDescs.ComponentRepository._numConnections == 0
		or not self._receptacleDescs.ComponentRepository._numConnections then
			-- we do not have any repositories. This could be the loading of the first one.
			-- if the component we're trying to load is a standard repository and the file is 
			-- already in the HD, load it
			self.utils:verbosePrint("ComponentContainer::ComponentLoader::Load : There aren't any known" ..
									" repositories. Checking if this component is a repository to be " ..
									"loaded from the hd...")
			if componentId.name == "ComponentRepository" then
				local f = io.open("../repository/ComponentRepository.lua", "rb")
				if f then
					-- saves in cache
					self.utils:verbosePrint("ComponentContainer::ComponentLoader::Load : Caching " ..
											nameVersion .. "...")
--                     local octetSeq = f:read("*all")
--                     local cachefile = io.open("../container/" ..
--                                         nameVersion .. "/" ..
--                                         componentId.name ..
--                                         ".lua", "rb")
--                     if not cachefile then
--                         os.execute("mkdir \"../container/" .. nameVersion .. "\"")
--                         cachefile = assert(io.open("../container/" .. nameVersion .. "/" .. 
--                         	 componentId.name .. ".lua", "wb"), "IDL:scs/container/LoadFailure:1.0")
--                         cachefile:write(octetSeq)
--                     end
--                     cachefile:close()
					self.componentFullDescriptions[nameVersion] = {}
					self.componentFullDescriptions[nameVersion].description = {
						id = componentId, 
						entry_point = "scs.repository.ComponentRepository", 
						shared = true, 
						extension = "lua"}
					firstRepository = true
				end
				f:close()
			end
			if componentId.name == "OpenbusClientInterceptor" then
				local f = io.open("../interceptor/OpenbusClientInterceptor.lua", "rb")
				if f then
					self.utils:verbosePrint("ComponentContainer::ComponentLoader::Load : Caching " ..
											nameVersion .. "...")
					self.componentFullDescriptions[nameVersion] = {}
					self.componentFullDescriptions[nameVersion].description = {
						id = componentId, 
						entry_point = "scs.interceptor.OpenbusClientInterceptor", 
						shared = false, 
						extension = "lua"}
				end
				f:close()
			end
			if componentId.name == "OpenbusServerInterceptor" then
				local f = io.open("../interceptor/OpenbusServerInterceptor.lua", "rb")
				if f then
					self.utils:verbosePrint("ComponentContainer::ComponentLoader::Load : Caching " ..
											nameVersion .. "...")
					self.componentFullDescriptions[nameVersion] = {}
					self.componentFullDescriptions[nameVersion].description = {
						id = componentId, 
						entry_point = "scs.interceptor.OpenbusServerInterceptor", 
						shared = false, 
						extension = "lua"}
				end
				f:close()
			end
--[[
			if componentId.name == "SuspendContainerInterceptor" then
				local f = io.open("../interceptor/SuspendContainerInterceptor.lua", "rb")
				if f then
					self.utils:verbosePrint("ComponentContainer::ComponentLoader::Load : Caching " ..
											nameVersion .. "...")
					self.componentFullDescriptions[nameVersion] = {}
					self.componentFullDescriptions[nameVersion].description = {
						id = componentId, 
						entry_point = "scs.interceptor.SuspendContainerInterceptor", 
						shared = false, 
						extension = "lua"}
				end
				f:close()
			end
--]]
		else
			self:checkReposForComponent(componentId)
		end
	end

	local description = 1
	if self.componentFullDescriptions[nameVersion] then
		description = self.componentFullDescriptions[nameVersion].description
		-- We already have the description. Checking if the component is already loaded...
		self.utils:verbosePrint("ComponentContainer::ComponentLoader::Load : Testing if the component " ..
								nameVersion .. " is already loaded...")
		if not self.componentFullDescriptions[nameVersion].factory then
			-- loads the lua component
			if description.extension == "zip" then
				self.utils:verbosePrint("ComponentContainer::ComponentLoader::Load : Unzipping...")
				local err = os.execute("unzip -o ./container/" ..
										nameVersion ..
										"/" ..
										nameVersion ..
										".zip -d ./container/" ..
										nameVersion)
				assert(err == 0, "IDL:scs/container/LoadFailure:1.0")
			end
			self.utils:verbosePrint("ComponentContainer::ComponentLoader::Load : Loading LUA component " ..
									description.entry_point)
			local factory = assert(require(description.entry_point), "IDL:scs/container/LoadFailure:1.0")
			self.componentFullDescriptions[nameVersion].factory = factory
		end
	else
		error{"IDL:scs/container/LoadFailure:1.0"}
	end

	-- Component is loaded, create instance
	self.utils:verbosePrint("ComponentContainer::ComponentLoader::Load : Trying to create an instance.")
--   table.insert(arg, self.props.use_openbus.value)
	local componentRef = self.componentFullDescriptions[nameVersion].factory:create(arg)
	assert(componentRef, "IDL:scs/container/LoadFailure:1.0")
	self.utils:verbosePrint("ComponentContainer::ComponentLoader::Load : Instance created.")

	-- creates handle
	self.utils:verbosePrint("ComponentContainer::ComponentLoader::Load : Creating handle...")
	local id = self.maxInstanceId + 1
	self.maxInstanceId = id
	self.numComponents = self.numComponents + 1
	self.utils:verbosePrint("ComponentContainer::ComponentLoader::Load : New instance id is " .. id .. ".")
	
	if not self.componentFullDescriptions[nameVersion].handles then
		self.componentFullDescriptions[nameVersion].handles = {}
	end
	self.componentFullDescriptions[nameVersion].handles[id] = { id = componentId, 
																instance_id = id, 
																cmp = componentRef}
	self.componentHandles[id] = self.componentFullDescriptions[nameVersion].handles[id]

--[[	
	if firstRepository then
		self.utils:verbosePrint("ComponentContainer::ComponentLoader::Load : Connecting first repository...")
		self.IReceptacles:connect("ComponentRepository", componentRef)
	end
--]]

	self.utils:verbosePrint("ComponentContainer::ComponentLoader::Load : Finished.")
	return self.componentFullDescriptions[nameVersion].handles[id]
end

--
-- Description: Unloads a component from the container.
-- Parameter handle: Component's handle.
-- Throws: IDL:InvalidParameter, IDL:ComponentNotFound exceptions
--
function CpnLoader:unload(handle)
	self = self.context
	self.utils:verbosePrint("ComponentContainer::ComponentLoader::Unload")
	assert(self.componentHandles[handle.instance_id], "IDL:scs/container/ComponentNotFound:1.0")
	self.utils:verbosePrint("ComponentContainer::ComponentLoader::Unload : Shutting down the component...")
	local status, err = oil.pcall(self.componentHandles[handle.instance_id].cmp.shutdown, 
									self.componentHandles[handle.instance_id].cmp)
	if not status then
		self.utils:verbosePrint("ComponentContainer::ComponentLoader::Unload : Unable to shutdown component " ..
								handle.id.name .. ".\nError: " .. err)
	else
		self.utils:verbosePrint("ComponentContainer::ComponentLoader::Unload : Component seems to have" ..
								" shut down correctly.")
	end
	self.utils:verbosePrint("ComponentContainer::ComponentLoader::Unload : Erasing references...")
	self.componentHandles[handle.instance_id] = nil
	self.componentFullDescriptions[handle.id.name .. handle.id.version].handles[handle.instance_id] = nil
	self.numComponents = self.numComponents - 1
	self.utils:verbosePrint("ComponentContainer::ComponentLoader::Unload : Finished.")
end

--------------------------------------------------------------------------------
-- ComponentCollection Facet
--------------------------------------------------------------------------------

local CpnCollection = oo.class{}

function CpnCollection:__init()
	return oo.rawnew(self, {})
end

--
-- Description: Returns all handles of a specific component of the collection.
-- Parameter componentId: Component's identifier.
-- Return Value: Handle collection.
--
function CpnCollection:getComponent(componentId)
	self = self.context
	self.utils:verbosePrint("ComponentContainer::ComponentCollection::GetComponent")
	self.utils:verbosePrint("ComponentContainer::ComponentCollection::GetComponent : Component name and version are " ..
							componentId.name .. ", " .. componentId.version)
	self.utils:verbosePrint("ComponentContainer::ComponentCollection::GetComponent : Finished.")
	return self.utils:convertToArray(self.componentFullDescriptions[componentId.name..componentId.version].handles)
end

--
-- Description: Returns all handles of all components of the collection.
-- Return Value: Handle collection.
--
function CpnCollection:getComponents()
	self = self.context
	self.utils:verbosePrint("ComponentContainer::ComponentCollection::GetComponents")
	self.utils:verbosePrint("ComponentContainer::ComponentCollection::GetComponents : Finished.")
	return self.utils:convertToArray(self.componentHandles)
end

--------------------------------------------------------------------------------
-- ComponentInterception Facet
--------------------------------------------------------------------------------

-- DESCRICAO:
-- 0) container, na inicializacao, cria e instala componente gerenciador de interceptadores.
-- 1) EN pede para container instalar interceptadores do openbus
-- 2) faceta do container de gerenciamento de interceptadores cuida da instalacao de novos interceptadores e organizacao de fila. Instala plugando no receptaculo do gerenciador. Como mudar ordem?
-- 3) container, na inicializacao, instala interceptadores do openbus
-- 4) componente gerenciador de interceptadores cuida de chamar outros interceptadores em sequencia.
-- 5) componente gerenciador de interceptadores usa o receptaculo de lista
-- 6) faceta do container, ao receber um pedido de load, unload ou reorder, deve fazer o q?
-- 7) como garantir q nao haja problemas ao modificar a lista do receptaculo? usar boolean de lock? ver se oil cuida?

local CpnInterception = oo.class{}

function CpnInterception:__init()
	return oo.rawnew(self, {})
end

-- 			ComponentHandle loadInterceptor (in ComponentId id, in StringSeq args, in unsigned long position)
-- 				raises (ListLockFail, ComponentNotFound, ComponentAlreadyLoaded, LoadFailure);

--
-- Description: Installs an interceptor at this container.
-- Parameter id: The interceptor's ComponentId.
-- Parameter args: The arguments to be passed to the interceptor.
-- Parameter position: The interceptor position at the ordered list.
-- Return Value: New interceptor's handle.
--
function CpnInterception:loadInterceptor(id, args, position, type)
	self = self.context
	self.utils:verbosePrint("ComponentContainer::ComponentInterception::LoadInterceptor")
	if type ~= string.lower("server") and type ~= string.lower("client") then
		error{"IDL:scs/container/LoadFailure:1.0"}
	end
	local itcHandle = self.ComponentLoader:load(id, args)
	self.IM:addInterceptor(itcHandle, position, type)
	self.utils:verbosePrint("ComponentContainer::ComponentInterception::LoadInterceptor: Finished.")
	return itcHandle
end

-- 			void unloadInterceptor (in ComponentHandle handle)
-- 				raises (InterceptorNotInstalled, ListLockFail, ComponentNotFound);

--
-- Description: Removes an interceptor from an especified component.
-- Parameter handle: Interceptor's component handle.
--
function CpnInterception:unloadInterceptor(handle)
	self = self.context
	self.utils:verbosePrint("ComponentContainer::ComponentInterception::UnloadInterceptor")
	self.utils:verbosePrint("ComponentContainer::ComponentInterception::UnloadInterceptor : Instance id is " ..
			tostring(handle.instance_id) .. ".")
	assert(self.componentHandles[id], "IDL:scs/container/ComponentNotFound:1.0")
	self.IM:removeInterceptor(handle.instance_id)
	self.CpnLoader:unload(handle)
	self.utils:verbosePrint("ComponentContainer::ComponentInterception::LoadInterceptor: Finished.")
	return itcHandle
end

-- 			void changePosition (in unsigned long instance_id, in unsigned long position)
-- 				raises (InterceptorNotInstalled, ListLockFail);

--
-- Description: Changes the position of an interceptor.
-- Parameter id: Interceptor's instance identifier.
-- Parameter position: Its new position.
--
function CpnInterception:changePosition(id, position)
	self = self.context
	self.utils:verbosePrint("ComponentContainer::ComponentInterception::ChangePosition")
	self.utils:verbosePrint("ComponentContainer::ComponentInterception::ChangePosition: Instance id is " ..
			tostring(id) .. ".")
	assert(self.componentHandles[id], "IDL:scs/container/ComponentNotFound:1.0")
	self.IM:changePosition(id, position)
	self.utils:verbosePrint("ComponentContainer::ComponentInterception::ChangePosition: Finished.")
end

-- 			unsigned long getInterceptorPosition (in unsigned long instance_id)
-- 				raises (InterceptorNotInstalled, ListLockFail);

--
-- Description: Returns the present position of an interceptor.
-- Parameter id: Interceptor's instance identifier.
-- Return Value: an unsigned long.
--
function CpnInterception:getInterceptorPosition(id)
	self = self.context
	self.utils:verbosePrint("ComponentContainer::ComponentInterception::GetInterceptorPosition")
	assert(self.componentHandles[id], "IDL:scs/container/ComponentNotFound:1.0")
	local ret = self.IM:getInterceptorPosition(id)
	self.utils:verbosePrint("ComponentContainer::ComponentInterception::GetInterceptorPosition: Finished.")
	return ret
end

-- 			InterceptorIds getInterceptorsOrder ();

--
-- Description: Returns the client interceptor ids, in the order in which they are executed at the moment.
-- Return Value: a list with unsigned longs.
--
function CpnInterception:getClientInterceptorsOrder()
	self = self.context
	self.utils:verbosePrint("ComponentContainer::ComponentInterception::GetClientInterceptorsOrder")
	local ret = self.IM:getInterceptorsOrder("client")
	self.utils:verbosePrint("ComponentContainer::ComponentInterception::GetClientInterceptorsOrder: Finished.")
	return ret
end

--
-- Description: Returns the server interceptor ids, in the order in which they are executed at the moment.
-- Return Value: a list with unsigned longs.
--
function CpnInterception:getServerInterceptorsOrder()
	self = self.context
	self.utils:verbosePrint("ComponentContainer::ComponentInterception::GetServerInterceptorsOrder")
	local ret = self.IM:getInterceptorsOrder("server")
	self.utils:verbosePrint("ComponentContainer::ComponentInterception::GetServerInterceptorsOrder: Finished.")
	return ret
end


--------------------------------------------------------------------------------
-- ComponentSuspension Facet
--------------------------------------------------------------------------------

local CpnSuspension = oo.class{}

function CpnSuspension:__init()
	return oo.rawnew(self, {})
end

--
-- Description: Suspends all components via a SuspendContainerInterceptor. Received calls are yielded.
--
function CpnSuspension:suspend()
	self = self.context
	self.utils:verbosePrint("ComponentContainer::ComponentSuspension::Suspend")
-- 	self.SCIServer:changeBehaviour(-1)
-- 	self.SCIClient:changeBehaviour(-1)
	self.IM:changeBehaviour(-1)
	self.utils:verbosePrint("ComponentContainer::ComponentSuspension::Suspend: Finished.")
end

--
-- Description: Halts all components via a SuspendContainerInterceptor. Received calls receive a ContainerHalted exception.
--
function CpnSuspension:halt()
	self = self.context
	self.utils:verbosePrint("ComponentContainer::ComponentSuspension::Halt")
-- 	self.SCIServer:changeBehaviour(1)
-- 	self.SCIClient:changeBehaviour(1)
	self.IM:changeBehaviour(1)
	self.utils:verbosePrint("ComponentContainer::ComponentSuspension::Halt: Finished.")
end

--
-- Description: Resumes normal operation of components.
--
function CpnSuspension:resume()
	self = self.context
	self.utils:verbosePrint("ComponentContainer::ComponentSuspension::Resume")
-- 	self.SCIServer:resume()
-- 	self.SCIClient:resume()
 	self.IM:resume()
	self.utils:verbosePrint("ComponentContainer::ComponentSuspension::Resume: Finished.")
end

--
-- Description: Returns the current container state.
-- Return Value: A number. 0 means normal operation, a positive number means a halt and a negative number means a suspension.
--
function CpnSuspension:getStatus()
	self = self.context
	self.utils:verbosePrint("ComponentContainer::ComponentSuspension::GetStatus")
-- 	local status = self.SCIServer:getBehaviour()
	local status = self.IM:getBehaviour()
	self.utils:verbosePrint("ComponentContainer::ComponentSuspension::GetStatus: Status is " .. status)
	self.utils:verbosePrint("ComponentContainer::ComponentSuspension::GetStatus: Finished.")
	return status
end

--------------------------------------------------------------------------------
-- IComponent Facet
--------------------------------------------------------------------------------

--
-- Description: Starts the Component Container.
--
local function startup(self)
	self = self.context
	self.utils:verbosePrint("ComponentContainer::IComponent::Startup")

	if not self.started then
-- 		if not self.SCIClient or not self.SCIServer then
-- 			error("IDL:scs/core/StartupFailed:1.0")
-- 		end

		if self.props.initfile then
			self.utils:verbosePrint("ComponentContainer::IComponent::Startup : Executing init file "
									.. self.props.initfile.value .. "...")
			local func, err = loadfile("../container/" .. self.props.initfile.value)
			if not func then
				self.utils:verbosePrint("Error loading init file! Error: " .. err)
				error("IDL:scs/core/StartupFailed:1.0")
			end
			oil.newthread(func)
		end

		self.started = true
		self.utils:verbosePrint("ComponentContainer::IComponent::Startup : Finished.")
	else
		self.utils:verbosePrint("ComponentContainer::IComponent::Startup : This container is already " ..
								"started. Done nothing.")
	end
end


local function reverseShutdown(self, handles, init)
	local i, handle = next(handles, init)
	if not i then return end
	reverseShutdown(self, handles, i)
	--TODO: substituir pcall por deferred do oil
	local status, err = oil.pcall(handle.cmp.shutdown, handle.cmp)
	if not status then 
		self.utils:verbosePrint("ComponentContainer::IComponent::Shutdown : Error trying to shutdown" ..
								" component " .. handle.id.name .. " of instance id " .. 
								handle.instance_id .. ": " .. err)
	else
		self.utils:verbosePrint("ComponentContainer::IComponent::Shutdown : Component " .. handle.id.name .. 
				" of instance id " .. handle.instance_id .. " has shutdown without errors.")
	end
end


--
-- Description: Shuts down the Component Container.
--
local function shutdown(self)
	self = self.context
	self.utils:verbosePrint("ComponentContainer::IComponent::Shutdown")

	self.utils:verbosePrint("ComponentContainer::IComponent::Shutdown : Unregistering at the execution node...")
	-- Unregisters at the execution node
	local status, err = oil.pcall(self.ContainerManager.unregisterContainer, 
									self.ContainerManager, self.name)
	if not status then 
		self.utils:verbosePrint("ComponentContainer::IComponent::Shutdown : Could not unregister at " ..
								"execution node.\nError: " .. err)
	end

	self.utils:verbosePrint("ComponentContainer::IComponent::Shutdown : Trying to shutdown all components...")
	reverseShutdown(self, self.componentHandles)

	self.utils:verbosePrint("ComponentContainer::IComponent::Shutdown : Erasing cache files...")
	-- erases cache's files
	for index, fullDescription in pairs(self.componentFullDescriptions) do
		os.remove(fullDescription.description.id.name .. fullDescription.description.id.version ..
				"." .. fullDescription.description.extension)
	end
	
	local maxWait = 15
	if self.props.timeout then
		local maxWait = tonumber(self.props.timeout.value)
	end

	local numWaits = 1
	self.utils:verbosePrint("ComponentContainer::IComponent::Shutdown : Waiting " .. maxWait ..
							" seconds for components to shutdown...")
	while numWaits <= maxWait do
		oil.sleep(1)
		self.utils:verbosePrint("ComponentContainer::IComponent::Shutdown : Waited " ..
								numWaits .. " second(s).")
		numWaits = numWaits + 1
	end
	
	orb:deactivate(self.IComponent)
	orb:deactivate(self.IReceptacles)
	orb:deactivate(self.IMetaInterface)
	orb:deactivate(self.ComponentLoader)
	orb:deactivate(self.ComponentCollection)
	orb:deactivate(self.ComponentInterception)
	orb:deactivate(self.ComponentSuspension)
	orb:deactivate(self.ComponentHelp)
	orb:deactivate(self.ComponentProperties)
	orb:shutdown()
	self.utils:verbosePrint("ComponentContainer::IComponent::Shutdown : Exiting.")
	os.exit()
end

--------------------------------------------------------------------------------
-- ComponentContainer Factory
--------------------------------------------------------------------------------

local containerFactory = comp.Template{
	IComponent						= port.Facet,
	IReceptacles   				= port.Facet,
	IMetaInterface 				= port.Facet,
	ComponentLoader				= port.Facet,
	ComponentCollection		= port.Facet,
	ComponentInterception	= port.Facet,
	ComponentSuspension		= port.Facet,
	ComponentHelp					= port.Facet,
	ComponentProperties		=	port.Facet,
	ContainerManager			= port.Receptacle,
	ComponentRepository		= port.ListReceptacle,
	ComponentHelpRecept		= port.ListReceptacle,
}{	CtnBase,
	IComponent						= scs.Component,
	IReceptacles   				= scs.Receptacles,
	IMetaInterface 				= scs.MetaInterface,
	ComponentLoader     	= CpnLoader,
	ComponentCollection		= CpnCollection,
	ComponentInterception	=	CpnInterception,
	ComponentSuspension		= CpnSuspension,
	ComponentHelp	    		= help.CpnHelp,
	ComponentProperties		= scsprops.CpnProperties,
}

local descriptions = {}
descriptions.IComponent 						= {}
descriptions.IReceptacles						= {}
descriptions.IMetaInterface					= {}
descriptions.ComponentLoader				=	{}
descriptions.ComponentCollection		= {}
descriptions.ComponentInterception	= {}
descriptions.ComponentSuspension		= {}
descriptions.ComponentHelp					= {}
descriptions.ComponentProperties		= {}

descriptions.ContainerManager				= {}
descriptions.ComponentRepository		= {}
descriptions.ComponentHelpRecept		= {}

-- facet descriptions
descriptions.IComponent.name 											= "IComponent"
descriptions.IComponent.interface_name 						= "IDL:scs/core/IComponent:1.0"

descriptions.IReceptacles.name 										= "IReceptacles"
descriptions.IReceptacles.interface_name 					= "IDL:scs/core/IReceptacles:1.0"

descriptions.IMetaInterface.name 									= "IMetaInterface"
descriptions.IMetaInterface.interface_name				= "IDL:scs/core/IMetaInterface:1.0"

descriptions.ComponentLoader.name 								= "ComponentLoader"
descriptions.ComponentLoader.interface_name				= "IDL:scs/container/ComponentLoader:1.0"

descriptions.ComponentCollection.name 						= "ComponentCollection"
descriptions.ComponentCollection.interface_name 	= "IDL:scs/container/ComponentCollection:1.0"

descriptions.ComponentInterception.name 					= "ComponentInterception"
descriptions.ComponentInterception.interface_name = "IDL:scs/container/ComponentInterception:1.0"

descriptions.ComponentSuspension.name	 						= "ComponentSuspension"
descriptions.ComponentSuspension.interface_name 	= "IDL:scs/container/ComponentSuspension:1.0"

descriptions.ComponentHelp.name 									= "ComponentHelp"
descriptions.ComponentHelp.interface_name 				= "IDL:scs/auxiliar/ComponentHelp:1.0"

descriptions.ComponentProperties.name 						= "ComponentProperties"
descriptions.ComponentProperties.interface_name 	= "IDL:scs/auxiliar/ComponentProperties:1.0"

-- receptacle descriptions
descriptions.ContainerManager.name 								= "ContainerManager"
descriptions.ContainerManager.interface_name			= "IDL:scs/execution_node/ContainerManager:1.0"
descriptions.ContainerManager.is_multiplex				= false

descriptions.ComponentRepository.name 						= "ComponentRepository"
descriptions.ComponentRepository.interface_name 	= "IDL:scs/repository/ComponentRepository:1.0"
descriptions.ComponentRepository.is_multiplex			= true

descriptions.ComponentHelpRecept.name 						= "ComponentHelpRecept"
descriptions.ComponentHelpRecept.interface_name		= "IDL:scs/auxiliar/ComponentHelp:1.0"
descriptions.ComponentHelpRecept.is_multiplex			= true

-- component id
local componentId = {}
componentId.name = "ComponentContainer"
componentId.version = 1

--------------------------------------------------------------------------------
-- Exporting
--------------------------------------------------------------------------------

oil.main(function()
	-- starts to wait for remote calls
  	oil.newthread(orb.run, orb)

	-- must be global so init file can access it
	ctnInst = scs.newComponent(containerFactory, descriptions, componentId)
	-- providing instance to init file, if needed
	_G.ctnInst = ctnInst
	-- overriding IComponent methods
	ctnInst.IComponent.startup = startup
	ctnInst.IComponent.shutdown = shutdown
	ctnInst.props = props

	-- argument treatment
	for index, arg in ipairs(_G.arg) do
		if index == 1 then
			ctnInst.name = arg
			ctnInst.utils.fileName = arg
			ctnInst.ComponentHelp.componentName = ctnInst.name
		else
			if string.upper(tostring(arg)) == "-L" then
				ctnInst.utils:verbosePrint("ComponentContainer: Loading file " .. _G.arg[index+1])
				assert(loadfile("../container/".._G.arg[index+1]))()
			end
			if string.upper(tostring(arg)) == "-O" then
				ctnInst.utils:verbosePrint("ComponentContainer: Using OpenBus")
				ctnInst.props.use_openbus.value = "true"
			end
		end
	end
	assert(ctnInst.name, "IDL:scs/core/InvalidName:1.0")

	if not ctnInst.props.platform then
		ctnInst.utils:verbosePrint("Properties file must contain platform property!")
		ctnInst.IComponent:shutdown()
	end

	-- install interceptor manager both as client and server interceptor
	orb:setclientinterceptor(ctnInst.IM)
	orb:setserverinterceptor(ctnInst.IM)

-- 	local suspendPosition = 1

	-- OPENBUS SETUP
	if ctnInst.props.use_openbus.value == "true" then
-- 		suspendPosition = 2

		ctnInst.props.accessControlServerHost = {name = "accessControlServerHost", read_only = true}
		ctnInst.props.accessControlServerHost.value = ctnInst.props.accessControlServerHostName.value .. ":" ..
				ctnInst.props.accessControlServerHostPort.value
		local credentialHolder = CredentialHolder()
		ctnInst.connectionManager =  ServiceConnectionManager(
				ctnInst.props.accessControlServerHost.value, credentialHolder,
				ctnInst.props.privateKeyFile.value,
				ctnInst.props.accessControlServiceCertificateFile.value)
	
		-- Loads access control interface
		idldir = os.getenv("CORBA_IDL_DIR")
		local idlfile = idldir .. "/access_control_service.idl"
		orb:loadidlfile (idlfile)

		-- Obtains a reference to the access control service
		ctnInst.accessControlService = ctnInst.connectionManager:getAccessControlService()
		if ctnInst.accessControlService == nil then
			ctnInst.utils:verbosePrint("Could not resolve access control service！")
			ctnInst.IComponent:shutdown()
		end

		-- installs client interceptor (intercepts outgoing calls)
		local CONF_DIR = os.getenv("CONF_DIR")
		local interceptorsConfig = assert(loadfile(CONF_DIR.."/advanced/InterceptorsConfiguration.lua"))()
		ctnInst.ComponentInterception:loadInterceptor({name = "OpenbusClientInterceptor", version = 1.0}, 
				{interceptorsConfig, credentialHolder}, 1, "client")

		-- Adds info about interfaces to be checked
--[[
 		interceptorsConfig.interfaces = {
 			{
 				interface = "IDL:scs/container/ComponentLoader:1.0",
 				excluded_ops = {}
 			},
 			{
 				interface = "IDL:scs/container/ComponentInterception:1.0",
 				excluded_ops = {}
 			},
 			{
 				interface = "IDL:scs/container/ComponentSuspension:1.0",
 				excluded_ops = {}
 			}
 		}
--]]

		-- installs server interceptor (intercepts incoming call on specified facets)
		ctnInst.ComponentInterception:loadInterceptor({name = "OpenbusServerInterceptor", version = 1.0}, 
				{interceptorsConfig, ctnInst.accessControlService, ctnInst}, 1, "server")

		-- autenticates container, connecting it to the bus
		local success = ctnInst.connectionManager:connect("ComponentContainer", nil)
		if not success then
			ctnInst.utils:verbosePrint("Could not connect to bus！")
			ctnInst.IComponent:shutdown()
		end
		
		-- offers service at registryService
		oil.newthread(
			function (ctnInst, idldir)
				local idlfile = idldir .. "/registry_service.idl"
				orb:loadidlfile (idlfile)
				ctnInst.registryService = ctnInst.accessControlService:getRegistryService()
				local success, registryIdentifier = ctnInst.registryService:register({type = "ComponentContainer", 
					description = "Component container from machine " .. ctnInst.props.host.value, 
					properties = {host = ctnInst.props.host.value}, member = ctnInst.IComponent })
				if not success then
					ctnInst.utils:verbosePrint("ComponentContainer::IComponent::Startup : Could not register at registry service！")
				end
				ctnInst.props.registryIdentifier = {name = "", read_only = true}
				ctnInst.props.registryIdentifier.value = registryIdentifier
			end,
			ctnInst, idldir
		)
		-- END OF OPENBUS SETUP
	end
	
	-- creating suspension interceptor
-- 	SCICComp = ctnInst.ComponentInterception:loadInterceptor({name = "SuspendContainerInterceptor", version = 1.0}, 
-- 			{ctnInst}, suspendPosition, "client")
-- 	ctnInst.SCIClient = SCICComp.cmp.context
-- 	SCISComp = ctnInst.ComponentInterception:loadInterceptor({name = "SuspendContainerInterceptor", version = 1.0},
-- 			{ctnInst}, suspendPosition, "server")
-- 	ctnInst.SCIServer = SCISComp.cmp.context
-- 	SCICComp.cmp:startup()
-- 	SCISComp.cmp:startup()

	-- find and contact execution node
	f = assert(io.open(ctnInst.props.enIOR.value, "r"), "Error opening Execution Node's IOR file!")
	local enIOR = f:read("*all")
	f:close()
	
	enComponentProxy = orb:newproxy(enIOR, "IDL:scs/core/IComponent:1.0")

	local status, facet = oil.pcall( enComponentProxy.getFacet, 
									enComponentProxy, 
									"IDL:scs/execution_node/ContainerManager:1.0")
	if not status then 
		ctnInst.utils:verbosePrint("Unable to get execution node's container manager facet.\nError: " .. facet)
		ctnInst.IComponent:shutdown()
	end

	ctnMngFacet = orb:narrow(facet)
	ctnInst.ENConnId = ctnInst.IReceptacles:connect("ContainerManager", ctnMngFacet)

	-- registering
	status, err = oil.pcall( ctnMngFacet.registerContainer, 
									ctnMngFacet, 
									ctnInst.name,
									ctnInst.IComponent)
	if not status then 
		ctnInst.utils:verbosePrint("Unable to register at the execution node.\nError: " .. err)
		ctnInst.IComponent:shutdown()
	end
	
--	oil.writeto("../../tests/container/container.ior", orb:tostring(ctnInst.IComponent))
end)
