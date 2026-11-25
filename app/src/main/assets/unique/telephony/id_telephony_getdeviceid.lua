-- Legacy ID.Unique getDeviceId / getDeviceId(slot): IMEI for GSM phones, MEID for CDMA phones.
-- Read cell.unique.imei/meid.<slot+1> first, then the legacy names. Leave the real value when nothing is configured.
function after(hook, param)
	local ths = param:getThis()
	local ret = param:getResult()
	if ths == nil or ret == nil then
		return false
	end

	local slot = param:tryGetArgumentInt(0, 0)
	local idx = tostring(slot + 1)
	local typ = param:getSettingInt("cell.phone.type." .. idx, 0)
	if typ == nil or typ == 0 then
		typ = ths:getPhoneType()
	end

	local fake
	if typ == 2 then
		fake = param:getSetting("cell.unique.meid." .. idx)
		if param:stringIsEmpty(fake) and slot == 0 then
			fake = param:getSettingReMap("unique.gsm.meid", "value.meid")
		end
	else
		fake = param:getSetting("cell.unique.imei." .. idx)
		if param:stringIsEmpty(fake) and slot == 0 then
			fake = param:getSettingReMap("unique.gsm.imei", "value.imei")
		end
	end

	if param:stringIsEmpty(fake) then
		return false
	end

	param:setResult(fake)
	return true, ret, fake
end
