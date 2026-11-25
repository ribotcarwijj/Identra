-- CellIdentity{Gsm,Wcdma,Lte,Tdscdma}.getMobileNetworkOperator(): MCC+MNC of the cell.
-- Uses cell.operator.numeric.id.1, else mcc.1 + mnc.1. Unknown cells return null and are left alone.
function after(hook, param)
	local ret = param:getResult()
	if ret == nil then
		return false
	end

	local fake = param:getSetting("cell.operator.numeric.id.1")
	if param:stringIsEmpty(fake) then
		local mcc = param:getSetting("cell.operator.mcc.1")
		local mnc = param:getSetting("cell.operator.mnc.1")
		if param:stringIsEmpty(mcc) or param:stringIsEmpty(mnc) then
			return false
		end
		fake = mcc .. mnc
	end
	if tostring(ret) == fake then
		return false
	end

	param:setResult(fake)
	return true, param:safe(ret), param:safe(fake)
end
