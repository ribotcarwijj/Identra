-- LAC / TAC / CDMA network id of a cell. Profile key cell.location.lac, legacy gsm.cell.location.lac or "LAC,CID".
function before(hook, param)
	local lacString = param:getSettingReMap("cell.location.lac", "gsm.cell.location.lac")
	if lacString ~= nil and tonumber(lacString) ~= nil then
	    param:setResult(tonumber(lacString))
	    return true
	end

	local setting = param:getSetting("LAC,CID")
	if setting ~= nil then
		local index = string.find(setting, ",", 1, true)
		if index ~= nil then
		    local fakeLac = tonumber(string.sub(setting, 1, index - 1))
		    if fakeLac ~= nil then
		        param:setResult(fakeLac)
		        return true
		    end
		end
	end
	return false
end
