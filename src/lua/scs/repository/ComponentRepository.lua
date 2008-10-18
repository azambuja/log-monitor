--
-- SCS - Software Component System
-- ComponentRepository.lua
-- Description: Component Repository component implementation
-- Version: 1.0
--

local oil		= require "oil"
local oo		= require "loop.base"
local comp 	= require "loop.component.base"
local port 	= require "loop.component.base"
local scs  	= require "scs.core.base"
local utils		= require "scs.core.utils"
local help 	= require "scs.auxiliar.componenthelp"

-- If we stored a broker instance previously, use it. If not, use the default broker
local orb = oil.orb or oil.init()

local dofile	= dofile
local string	= string
local assert	= assert
local os		= os
local loadfile	= loadfile
local print	= print
local tostring	= tostring
local error	= error

--------------------------------------------------------------------------------

module "scs.repository.ComponentRepository"

--------------------------------------------------------------------------------

--------------------------------------------------------------------------------
-- ComponentRepository Component
--------------------------------------------------------------------------------
local RepoBase = oo.class{
	componentFullDescriptions = {},
	componentDescriptions = {},
	containerComponent = {},
	props = {},
	containerInstanceId = -1,
	myInstanceId = -1,
	started = false,
	utils = false,
}

function RepoBase:__init()
	-- makes sure that table values are recreated for the instance. If not, base class values may 
	-- be modified.
	local inst = oo.rawnew(self, {})
	inst.utils = utils.Utils()
	inst.utils.verbose = true
	inst.utils.fileVerbose = true
	inst.utils.fileName = "repository"
	inst.componentFullDescriptions = {}
	inst.componentDescriptions = {}
	inst.containerComponent = {}
	inst.props = {}
	return inst
end

--------------------------------------------------------------------------------
-- ComponentRepository Facet
--------------------------------------------------------------------------------

local CpnRepository = oo.class{}

function CpnRepository:__init()
	return oo.rawnew(self, {})
end

--
-- Description: Installs a component.
-- Parameter ComponentId: Component's identifier.
-- Parameter EntryPoint: Component's entry point, i.e., what should be executed when loading it.
-- Parameter Shared: If one instance should be shared among all components.
-- Parameter File: Component's octet sequence.
-- Parameter Help: Component's help info.
-- Parameter Ext: Component's extension.
-- Throws: IDL:InvalidParameter IDL:ComponentAlreadyInstalled exceptions
--
function CpnRepository:install(componentId, entryPoint, shared, file, help, ext)
	self = self.context
	local utils = self.utils
	utils:verbosePrint("ComponentRepository::ComponentRepository::Install")

	local nameVersion = componentId.name .. componentId.version
	if not self.componentFullDescriptions[nameVersion] then
		self.componentFullDescriptions[nameVersion] = {}
		self.componentFullDescriptions[nameVersion].help = help
		self.componentFullDescriptions[nameVersion].octetSeq = file
		self.componentFullDescriptions[nameVersion].description = { id = componentId, 
																	entry_point = entryPoint, 
																	shared = shared, 
																	extension = ext}
		self.componentDescriptions[nameVersion] = self.componentFullDescriptions[nameVersion].description
		
		-- writes at the hd
--		local f = assert(io.open("./ComponentRepository/"..entryPoint, "wb"))
--		f:write(file)
--		f:close()
		utils:verbosePrint( "ComponentRepository::ComponentRepository::Install : Component " ..
							nameVersion .. " installed.")
	else
		utils:verbosePrint( "ComponentRepository::ComponentRepository::Install : Component " ..
							nameVersion .. " was already installed, launching exception.")
		error { "IDL:scs/repository/ComponentAlreadyInstalled:1.0" }
	end
	utils:verbosePrint("ComponentRepository::ComponentRepository::Install : Finished.")
end

--
-- Description: Uninstalls a component.
-- Parameter ComponentId: Component's identifier.
-- Throws: IDL:InvalidParameter IDL:ComponentNotInstalled exceptions
--
function CpnRepository:uninstall(componentId)
	self = self.context
	local utils = self.utils
	utils:verbosePrint("ComponentRepository::ComponentRepository::Uninstall")
	
	local nameVersion = componentId.name .. componentId.version
	if self.componentFullDescriptions[nameVersion] then
		self.componentFullDescriptions[nameVersion] = nil
		self.componentDescriptions[nameVersion] = nil
		utils:verbosePrint( "ComponentRepository::ComponentRepository::Uninstall : Component " ..
							nameVersion .. " uninstalled")
	else
		utils:verbosePrint( "ComponentRepository::ComponentRepository::Uninstall : Component " ..
							nameVersion .. " was not installed, launching exception")
		error { "IDL:scs/repository/ComponentNotInstalled:1.0" }
	end
	utils:verbosePrint("ComponentRepository::ComponentRepository::Uninstall : Finished.")
end

--
-- Description: Appends a file part to a file.
-- Parameter ComponentId: Component's identifier.
-- Parameter File: File part.
-- Throws: IDL:InvalidParameter exception
--
function CpnRepository:appendFile(componentId, file)
	self = self.context
	local utils = self.utils
	utils:verbosePrint("ComponentRepository::ComponentRepository::AppendFile")
	
	local nameVersion = componentId.name .. componentId.version
	if self.componentFullDescriptions[nameVersion] then
		self.componentFullDescriptions[nameVersion].octetSeq = self.componentFullDescriptions[nameVersion].octetSeq ..
																file
--		local f = io.open("./ComponentRepository/"..entryPoint, "rb")
--		if f ~= nil then
--			f = assert(io.open("./ComponentRepository/"..entryPoint, "ab"))
--			f:write(file)
--		end
--		f:close()
		utils:verbosePrint("ComponentRepository::ComponentRepository::AppendFile : File complemented.")
	end
	utils:verbosePrint("ComponentRepository::ComponentRepository::AppendFile : Finished.")
end

--
-- Description: Copies a component to a remote repository.
-- Parameter ComponentId: Component's Identifier.
-- Parameter Repository: Repository's ComponentRepository interface.
-- Throws: IDL:InvalidParameter, IDL:ComponentAlreadyInstalled, IDL:ComponentNotInstalled exceptions
--
function CpnRepository:copy(componentId, repository)
	self = self.context
	local utils = self.utils
	utils:verbosePrint("ComponentRepository::ComponentRepository::Copy")
	
	local nameVersion = componentId.name .. componentId.version
	if self.componentFullDescriptions[nameVersion] then
		local entry_point = componentFullDescriptions[nameVersion].description.entry_point
		local shared = componentFullDescriptions[nameVersion].description.shared
		local file = componentFullDescriptions[nameVersion].octetSeq
		local help = componentFullDescriptions[nameVersion].help
		local extension = componentFullDescriptions[nameVersion].extension
		local status, err = oil.pcall(  repository.install, 
										repository, 
										componentId, 
										entry_point, 
										shared, 
										file, 
										help, 
										extension)
		if not status then 
			utils:verbosePrint( "ComponentRepository::ComponentRepository::Copy : Error copying " ..
								"component: " .. err)
			error{err}
		end
		utils:verbosePrint("ComponentRepository::ComponentRepository::Copy : Component was copied.")
	else
		utils:verbosePrint( "ComponentRepository::ComponentRepository::Copy : Component is not installed." ..
							" Launching exception...")
		error{"IDL:scs/repository/ComponentNotInstalled:1.0"}
	end
	utils:verbosePrint("ComponentRepository::ComponentRepository::Copy : Finished.")
end

--
-- Description: Provides a component's file.
-- Parameter ComponentId: Component's identifier.
-- Throws: IDL:InvalidParameter, IDL:ComponentNotInstalled exceptions
--
function CpnRepository:getComponentFile(componentId)
	self = self.context
	local utils = self.utils
	utils:verbosePrint("ComponentRepository::ComponentRepository::GetComponentFile")

	local nameVersion = componentId.name .. componentId.version
	if not self.componentFullDescriptions[nameVersion] then
		utils:verbosePrint( "ComponentRepository::ComponentRepository::GetComponentFile : Component" ..
							" is not installed. Launching exception...")
		error{"IDL:scs/repository/ComponentNotInstalled:1.0"}
	end
	utils:verbosePrint( "ComponentRepository::ComponentRepository::GetComponentFile : Component " ..
						"provided. Finished.")
	return self.componentFullDescriptions[nameVersion].octetSeq
end

--
-- Description: Provides a component's file, by pieces.
-- Parameter ComponentId: Component's identifier.
-- Parameter Size: Part's size.
-- Parameter Start: Part's start point.
-- Return Value #1: Part of file.
-- Return Value #2: Boolean indicating if the file has reached it's end.
-- Throws: IDL:InvalidParameter, IDL:ComponentNotInstalled exceptions
--
function CpnRepository:getComponentFileByPieces(componentId, size, start)
	self = self.context
	local utils = self.utils
	utils:verbosePrint("ComponentRepository::ComponentRepository::GetComponentFileByPieces")

	local nameVersion = componentId.name .. componentId.version
	if not self.componentFullDescriptions[nameVersion] then
		utils:verbosePrint( "ComponentRepository::ComponentRepository::GetComponentFileByPieces :" ..
							" Component is not installed. Launching exception...")
		error{"IDL:scs/repository/ComponentNotInstalled:1.0"}
	end
	utils:verbosePrint( "ComponentRepository::ComponentRepository::GetComponentFile : Octetseq's " ..
						"size: " .. string.len(self.componentFullDescriptions[nameVersion].octetSeq))
	if string.len(self.componentFullDescriptions[nameVersion].octetSeq) > (start + size) then
		utils:verbosePrint( "ComponentRepository::ComponentRepository::GetComponentFile : Octetseq's" ..
							" not reached the end of file. Start: " .. start .. ", size: "..size)
		return  string.sub(  self.componentFullDescriptions[nameVersion].octetSeq, start, start + size - 1),
				false
	else
		utils:verbosePrint( "ComponentRepository::ComponentRepository::GetComponentFile : Octetseq's" ..
							" reached the end of file. Start: " .. start .. ", size: " .. 
							string.len(self.componentFullDescriptions[nameVersion].octetSeq))
		return  string.sub (  self.componentFullDescriptions[nameVersion].octetSeq, 
							start, 
							string.len(self.componentFullDescriptions[nameVersion].octetSeq) 
							), 
				true
	end
	utils:verbosePrint("ComponentRepository::ComponentRepository::GetComponentFileByPieces : Finished.")
end

--
-- Description: Provides the component's description.
-- Parameter ComponentId: Component's identifier.
-- Return Value: The description.
-- Throws: IDL:InvalidParameter, IDL:ComponentNotInstalled exceptions
--
function CpnRepository:getComponentDescription(componentId)
	self = self.context
	local utils = self.utils
	utils:verbosePrint("ComponentRepository::ComponentRepository::GetComponentDescription")

	local nameVersion = componentId.name .. componentId.version
	if not self.componentFullDescriptions[nameVersion] then
		utils:verbosePrint( "ComponentRepository::ComponentRepository::GetComponentDescription" ..
							" : Component is not installed. Launching exception...")
		error{"scs/repository/IDL:ComponentNotInstalled:1.0"}
	end
	utils:verbosePrint( "ComponentRepository::ComponentRepository::GetComponentDescription : " ..
						"Description provided. Finished.")
	return self.componentFullDescriptions[nameVersion].description
end

--
-- Description: Provides the descriptions of the installed components.
-- Return Value: The description.
--
function CpnRepository:getInstalledComponents()
	local utils = self.context.utils
	utils:verbosePrint("ComponentRepository::ComponentRepository::GetInstalledComponents")
	utils:verbosePrint("ComponentRepository::ComponentRepository::GetInstalledComponents : Finished.")
	return utils:convertToArray(componentDescriptions)
end

--------------------------------------------------------------------------------
-- IComponent Facet
--------------------------------------------------------------------------------

--
-- Description: Starts the Component Repository.
--
local function startup(self)
	self = self.context
	local utils = self.utils
	utils:verbosePrint("ComponentRepository::IComponent::Startup")
	if not self.started then
		if self.props.initfile then
			utils:verbosePrint("ComponentRepository::IComponent::Startup : Executing init file " .. 
								self.props.initfile.value .. "...")
			oil.newthread(loadfile("../repository/" .. self.props.initfile.value))
		end
		self.started = true
		utils:verbosePrint("ComponentRepository::IComponent::Startup : Finished.")
	else
		utils:verbosePrint( "ComponentRepository::IComponent::Startup : This repository is already " ..
							"started. Done nothing.")
	end
end

--
-- Description: Shuts down the Component Repository.
--
local function shutdown(self)
	self = self.context
	local utils = self.utils
	utils:verbosePrint("ComponentRepository::IComponent::Shutdown")
	self.componentFullDescriptions = nil
	self.componentDescriptions = nil
	self.containerComponent = nil
	self.props = nil
	utils:verbosePrint("ComponentRepository::IComponent::Shutdown : Finished.")
end

--------------------------------------------------------------------------------
-- ComponentRepository Factory
--------------------------------------------------------------------------------

local repositoryFactory = comp.Template{
	IComponent					= port.Facet,
	IReceptacles   			= port.Facet,
	IMetaInterface 			= port.Facet,
	ComponentRepository	= port.Facet,
	ComponentHelp				= port.Facet,
}{	RepoBase,
	IComponent					= scs.Component,
	IReceptacles   			= scs.Receptacles,
	IMetaInterface 			= scs.MetaInterface,
	ComponentRepository = CpnRepository,
	ComponentHelp				= help.CpnHelp,
}

--oil.loadidlfile("../../../../idl/repository.idl")

local descriptions = {}
descriptions.IComponent 					= {}
descriptions.IReceptacles					= {}
descriptions.IMetaInterface				= {}
descriptions.ComponentRepository	= {}
descriptions.ComponentHelp				= {}

-- facet descriptions
descriptions.IComponent.name 				= "IComponent"
descriptions.IComponent.interface_name 		= "IDL:scs/core/IComponent:1.0"

descriptions.IReceptacles.name 				= "IReceptacles"
descriptions.IReceptacles.interface_name 	= "IDL:scs/core/IReceptacles:1.0"

descriptions.IMetaInterface.name 			= "IMetaInterface"
descriptions.IMetaInterface.interface_name	= "IDL:scs/core/IMetaInterface:1.0"

descriptions.ComponentRepository.name 			= "ComponentRepository"
descriptions.ComponentRepository.interface_name = "IDL:scs/repository/ComponentRepository:1.0"

descriptions.ComponentHelp.name 			= "ComponentHelp"
descriptions.ComponentHelp.interface_name   = "IDL:scs/auxiliar/ComponentHelp:1.0"

-- component id
local componentId = {}
componentId.name = "ComponentRepository"
componentId.version = 1

--------------------------------------------------------------------------------
-- Exporting
--------------------------------------------------------------------------------

local Factory = oo.class{	repositoryFactory = repositoryFactory, 
													descriptions = descriptions,
													componentId = componentId
						}

function Factory:create(args)
	local repoInst = scs.newComponent(self.repositoryFactory, self.descriptions, self.componentId)
	repoInst.utils:readProperties(repoInst.props, "../repository/Properties.txt")
	-- overriding IComponent methods
	repoInst.IComponent.startup = startup
	repoInst.IComponent.shutdown = shutdown
	repoInst.ComponentHelp.componentName = "ComponentRepository"
	return repoInst.IComponent
end

return Factory
