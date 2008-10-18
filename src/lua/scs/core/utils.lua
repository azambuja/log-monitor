--
-- SCS
-- utils.lua
-- Description: Basic SCS utils
-- Version: 1.0
--

local oo        		= require "loop.base"
local oil			= require "oil"

local module       	= module
local tostring     	= tostring
local type         	= type
local io 			= io
local string		= string
local assert		= assert
local os			= os
local print		= print
local pairs		= pairs

--------------------------------------------------------------------------------

module "scs.core.utils"

--------------------------------------------------------------------------------

--------------------------------------------------------------------------------

--
-- Util Class
-- Implementation of the utilitary class. It's use is not mandatory.
--
Utils = oo.class{ 	verbose 	= false,
					fileVerbose = false,
					newLog		= true,
					fileName 	= "",
				}

function Utils:__init()
	return oo.rawnew(self, {})
end

--
-- Description: Prints a message to the standard output and/or to a file.
-- Parameter message: Message to be delivered.
--
function Utils:verbosePrint(...)
	if self.verbose then
		print(...)
	end
	if self.fileVerbose then
		local f = io.open("../../../../logs/lua/"..self.fileName.."/"..self.fileName..".log", "at")
		if not f then
			os.execute("mkdir \"../../../../logs/lua/" .. self.fileName .. "\"")
			f = io.open("../../../../logs/lua/" .. self.fileName .. "/" .. self.fileName , "wt")
			-- do not throw error if f is nil
			if not f then return end
		end
		if self.newLog then
			f:write("\n-----------------------------------------------------\n")
			f:write(os.date().." "..os.time().."\n")
			self.newLog = false
		end
		f:write(...)
		f:write("\n")
		f:close()
	end
end	

--
-- Description: Reads a file with properties and store them at a table.
-- Parameter t: Table that will receive the properties.
-- Parameter file: File to be read.
--
function Utils:readProperties (t, file)
	local f = assert(io.open(file, "r"), "Error opening properties file!")
	while true do
		prop = f:read("*line")
		if prop == nil then
			break
		end
		self:verbosePrint("SCS::Utils::ReadProperties : Line: " .. prop)
		local a,b = string.match(prop, "%s*(%S*)%s*[=]%s*(.*)%s*")
		if a ~= nil then
			local readonly = false
			local first = string.sub(a, 1, 1)
			if first == '#' then
				a = string.sub(a, 2)
				readonly = true
			end
			t[a] = { name = a, value = b, read_only = readonly }
		end
	end
	f:close()
end

--
-- Description: Prints a table recursively.
-- 
function Utils:print_r (t, indent, done)
	done = done or {}
	indent = indent or 0
	if type(t) == "table" then
		for key, value in pairs (t) do
			io.write(string.rep (" ", indent)) -- indent it
			if type (value) == "table" and not done [value] then
			  done [value] = true
			  io.write(string.format("[%s] => table\n", tostring (key)));
			  io.write(string.rep (" ", indent+4)) -- indent it
			  io.write("(\n");
			  self:print_r (value, indent + 7, done)
			  io.write(string.rep (" ", indent+4)) -- indent it
			  io.write(")\n");
			else
			  io.write(string.format("[%s] => %s\n", tostring (key),tostring(value)))
			end
		end
	else
		io.write(t .. "\n")
	end
end

--
-- Description: Converts a table with an alphanumeric indice to an array.
-- Parameter message: Table to be converted.
-- Return Value: The array.
--
function Utils:convertToArray(inputTable)
	self:verbosePrint("SCS::Utils::ConvertToArray")
	local outputArray = {}
	local i = 1
	for index, item in pairs(inputTable) do
--		table.insert(outputArray, item)
		if index ~= "n" then
			outputArray[i] = item
			i = i + 1
		end
	end
	self:verbosePrint("SCS::Utils::ConvertToArray : Finished.")
	return outputArray
end

--
-- Description: Converts a string to a boolean.
-- Parameter str: String to be converted.
-- Return Value: The boolean.
--
function Utils:toBoolean(inputString)
    self:verbosePrint("SCS::Utils::StringToBoolean")
    local inputString = tostring(inputString)
    local result = false
    if string.find(inputString, "true") and string.len(inputString) == 4 then
        result = true
    end
    self:verbosePrint("SCS::Utils::StringToBoolean : " .. tostring(result) .. ".")
    self:verbosePrint("SCS::Utils::StringToBoolean : Finished.")
    return result
end
