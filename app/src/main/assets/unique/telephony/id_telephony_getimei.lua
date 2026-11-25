-- Legacy ID.Unique IMEI hooks (getImei, getDeviceIdGemini, getDeviceIdDs, getPrimaryImei).
-- Read the profile key cell.unique.imei.<slot+1> first (same value the Spoof.Telephony hooks use),
-- then the legacy names. Never invent a value: leave the real one when nothing is configured.
function after(hook, param)
	local ret = param:getResult()
	if ret == nil then
	    return false
	end

	local slot = param:tryGetArgumentInt(0, 0)
	local fake = param:getSetting("cell.unique.imei." .. tostring(slot + 1))
	if param:stringIsEmpty(fake) and slot == 0 then
		fake = param:getSettingReMap("unique.gsm.imei", "value.imei")
	end
	if param:stringIsEmpty(fake) then
	    return false
	end

	param:setResult(fake)
	return true, ret, fake
end
