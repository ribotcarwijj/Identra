-- Legacy ID.Unique MEID hooks (getMeid / getMeid(slot)).
-- Read cell.unique.meid.<slot+1> first, then the legacy names. Leave the real value when nothing is configured.
function after(hook, param)
	local ret = param:getResult()
	if ret == nil then
	    return false
	end

	local slot = param:tryGetArgumentInt(0, 0)
	local fake = param:getSetting("cell.unique.meid." .. tostring(slot + 1))
	if param:stringIsEmpty(fake) and slot == 0 then
		fake = param:getSettingReMap("unique.gsm.meid", "value.meid")
	end
	if param:stringIsEmpty(fake) then
	    return false
	end

	param:setResult(fake)
	return true, ret, fake
end
