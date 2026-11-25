-- TelephonyManager.getSimState() / getSimState(slotIndex).
-- Reads the profile code cell.sim.state.flag.<slot+1> (5 = SIM_STATE_READY), legacy names as fallback.
-- Only applies a valid numeric state; leaves the real value when nothing is configured.
-- (Old script compared an undefined variable 'state', so it always reported SIM_STATE_UNKNOWN (0)
--  whenever the real state was READY.)
function after(hook, param)
	local ret = param:getResult()
	if ret == nil then
		return false
	end

	local slot = param:tryGetArgumentInt(0, 0)
	local setting = param:getSetting("cell.sim.state.flag." .. tostring(slot + 1))
	if param:stringIsEmpty(setting) and slot == 0 then
		setting = param:getSettingReMap("gsm.sim.state", "phone.simstate")
	end
	if param:stringIsEmpty(setting) then
		return false
	end

	local fake = tonumber(setting)
	if fake == nil or fake < 0 or fake > 10 then
		return false
	end
	fake = math.floor(fake)
	if ret == fake then
		return false
	end

	--SIM_STATE_UNKNOWN=0 ABSENT=1 PIN_REQUIRED=2 PUK_REQUIRED=3 NETWORK_LOCKED=4 READY=5
	--NOT_READY=6 PERM_DISABLED=7 CARD_IO_ERROR=8 CARD_RESTRICTED=9 LOADED=10 (hidden)
	param:setResult(fake)
	return true, tostring(ret), tostring(fake)
end
