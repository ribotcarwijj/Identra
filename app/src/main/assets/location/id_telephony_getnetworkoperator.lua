-- MCC+MNC of the SIM / registered network (getSimOperator, getNetworkOperator, ServiceState.getOperatorNumeric)
-- Reads the profile values written by the profile generator (cell.operator.numeric.id.1, else mcc.1 + mnc.1).
function after(hook, param)
    local res = param:getResult()
    local name = "cell.operator.numeric.id.1"
    if not param:isForceSetting(name, res) then
        return false
    end

    local fake = param:getSetting(name)
    if param:stringIsEmpty(fake) then
        local mcc = param:getSetting("cell.operator.mcc.1")
        local mnc = param:getSetting("cell.operator.mnc.1")
        if param:stringIsEmpty(mcc) or param:stringIsEmpty(mnc) then
            return false
        end
        fake = mcc .. mnc
    end

    if res ~= nil and tostring(res) == fake then
        return false
    end

    param:setResult(fake)
    return true, param:safe(res), param:safe(fake)
end
