--------------------------------------------------------------------------------
-----------------------     ######   #####  ######      ------------------------
-----------------------     ##      ##      ##          ------------------------
-----------------------     ######  ##      ######      ------------------------
-----------------------         ##  ##          ##      ------------------------
-----------------------     ######   #####  ######      ------------------------
-----------------------                                 ------------------------
----------------------- SCS - Software Component System ------------------------
--------------------------------------------------------------------------------
-- SCS - Software Component System										--
-- debuglua.lua														--
-- Description: Script to make print all errors ocurred in executed file   	--
-- Authors: Hugo Roenick <hroenick@tecgraf.puc-rio.br>						--
--------------------------------------------------------------------------------

local file = (...)
local params = {select(2,...)}
local success, error = xpcall(function() return assert(loadfile(file))(unpack(params)) end, debug.traceback)
if not success then
	io.stdout:write(tostring(error),"\n")
	os.exit(1)
end
