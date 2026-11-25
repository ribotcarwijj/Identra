-- Deprecated CellIdentity{Gsm,Wcdma,Lte}.getMnc() (int). Integer.MAX_VALUE means unknown and is left alone.
function after(hook, param)
	local ret = param:getResult()
	if ret == nil or ret == 2147483647 then
		return false
	end

	local fake = tonumber(param:getSetting("cell.operator.mnc.1"))
	if fake == nil then
		return false
	end
	fake = math.floor(fake)
	if ret == fake then
		return false
	end

	param:setResult(fake)
	return true, tostring(ret), tostring(fake)
end
