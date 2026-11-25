-- CellIdentity{Gsm,Wcdma,Lte,Tdscdma,Nr}.getMccString(): MCC of the serving / neighbouring cell.
-- Without this the real network MCC leaks through getAllCellInfo() while the SIM operator is spoofed.
-- Unknown cells return null; those are left alone.
function after(hook, param)
	local ret = param:getResult()
	if ret == nil then
		return false
	end

	local fake = param:getSetting("cell.operator.mcc.1")
	if param:stringIsEmpty(fake) or tonumber(fake) == nil then
		return false
	end
	if tostring(ret) == fake then
		return false
	end

	param:setResult(fake)
	return true, param:safe(ret), param:safe(fake)
end
