-- CellIdentity{Gsm,Wcdma,Lte,Tdscdma,Nr}.getMncString(): MNC of the serving / neighbouring cell.
-- Unknown cells return null; those are left alone.
function after(hook, param)
	local ret = param:getResult()
	if ret == nil then
		return false
	end

	local fake = param:getSetting("cell.operator.mnc.1")
	if param:stringIsEmpty(fake) or tonumber(fake) == nil then
		return false
	end
	if tostring(ret) == fake then
		return false
	end

	param:setResult(fake)
	return true, param:safe(ret), param:safe(fake)
end
