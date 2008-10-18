enInst.utils:verbosePrint("ExecutionNode::InitFile : Entering init file...")
-- creates a default EN-only container
enInst.utils:verbosePrint("ExecutionNode::InitFile : Creating default container...")
local defCont = enInst.ExecutionNode:startContainer("ComponentContainer", {})
if not defCont then
	utils:verbosePrint("Error trying to create default container!")
else
	-- container is already started, getting component loader facet
	utils:verbosePrint("ExecutionNode::InitFile : Trying to get ComponentLoader facet...")
	local status, cmpLoader = oil.pcall(defCont.getFacet, defCont, "IDL:scs/container/ComponentLoader:1.0")
	if not status then
		utils:verbosePrint("Error trying to get ComponentLoader facet! Error: " .. cmpLoader)
	else
		cmpLoader = cmpLoader:_narrow()
		-- creates a default EN-only repository
		utils:verbosePrint("ExecutionNode::InitFile : Loading default repository...")
		local status, defRepHandle = oil.pcall( cmpLoader.load, 
												cmpLoader, 
												{name = "ComponentRepository",version = 1.0}, 
												{})
		if not status then
			utils:verbosePrint("Error trying to load repository component! Error: " .. defRepHandle)
		else
			-- starts it up
			utils:verbosePrint("ExecutionNode::InitFile : Starting up repository component...")
			local status, err = oil.pcall(defRepHandle.cmp.startup, defRepHandle.cmp)
			if not status then
				utils:verbosePrint("Error trying to startup default repository! Error: " .. err)
			end
			
			-- registers the default container
			utils:verbosePrint("ExecutionNode::InitFile : Registering default repository...")
			enInst.defaultContainer = {}
			enInst.defaultContainer.objComponent = defCont
			enInst.defaultContainer.objComponentLoader = cmpLoader
			
			enInst.defaultRepository = {}
			enInst.defaultRepository.handle = defRepHandle
		end
	end
end
utils:verbosePrint("ExecutionNode::InitFile : Init file executed.")
