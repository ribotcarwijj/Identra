-- CID / CI / CDMA base station id of a cell. Profile key cell.location.cid, legacy gsm.cell.location.cid or "LAC,CID".
function before(hook, param)
	local cidString = param:getSettingReMap("cell.location.cid", "gsm.cell.location.cid")
	if cidString ~= nil and tonumber(cidString) ~= nil then
	    param:setResult(tonumber(cidString))
	    return true
	end

	local setting = param:getSetting("LAC,CID")
	if setting ~= nil then
		local index = string.find(setting, ",", 1, true)
		if index ~= nil then
		    local fakeCid = tonumber(string.sub(setting, index + 1, -1))
		    if fakeCid ~= nil then
		        param:setResult(fakeCid)
		        return true
		    end
		end
	end
	return false
end
