--
-- SCS
-- componentproperties.lua
-- Description: Properties class
-- Version: 1.0
--

local oo = require "loop.base"
local assert = assert

--------------------------------------------------------------------------------

module "scs.auxiliar.componentproperties"

--------------------------------------------------------------------------------

--------------------------------------------------------------------------------
-- ComponentProperties Class
--------------------------------------------------------------------------------

CpnProperties = oo.class{ 
	context = false,
	componentName = "",
}

function CpnProperties:__init()
	return oo.rawnew(self, {})
end

function CpnProperties:findProperty(t, name)
	for index, prop in ipairs(t) do
		if prop.name == name then
			return prop
		end
	end
end

--
-- Description: Returns the component's properties.
-- Return Value: Array of properties.
--
function CpnProperties:getProperties()
	self = self.context
	self.utils:verbosePrint(self.componentName .. "::ComponentProperties::GetProperties")
	local ret = self.utils:convertToArray(self.props)
	self.utils:verbosePrint(self.componentName .. "::ComponentProperties::GetProperties : Finished.")
	return ret
end

--
-- Description: Returns one property.
-- Parameter name: Property's name.
-- Return Value: The property structure.
-- Throws: IDL:UndefinedProperty
--
function CpnProperties:getProperty(name)
	self = self.context
	self.utils:verbosePrint(self.componentName .. "::ComponentProperties::GetProperty")
	self.utils:verbosePrint(self.componentName .. "::ComponentProperties::GetProperty : Finished.")
	return self.props[name]
end

--
-- Description: Sets a property, be it already defined or not.
-- Parameter property: The property structure.
--
function CpnProperties:setProperty(property)
	self = self.context
	self.utils:verbosePrint(self.componentName .. "::ComponentProperties::SetProperty")
	local prop = self.props[property.name]
	if not prop then
		self.props[property.name] = property
	else
		if prop.read_only == true then
			error{"IDL:scs/auxiliar/ReadOnlyProperty:1.0"}
		else
			prop.value = property.value
			prop.read_only = self.utils:toBoolean(property.read_only)
		end
	end
	self.context.utils:verbosePrint(self.componentName .. "::ComponentProperties::SetProperty : Finished.")
end

