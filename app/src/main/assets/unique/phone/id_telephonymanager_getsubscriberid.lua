-- IMSI (getSubscriberId). Uses cell.unique.subscriber.id.N, else MCC + MNC + MSIN of the same SIM.
-- Never replaces the real value with nil when the profile has nothing for it.
function after(hook, param)
    local res = param:getResult()
    for i = 1, 2 do
        local nmeName = "cell.unique.subscriber.id." .. i
        local fake = param:getSetting(nmeName)
        if param:stringIsEmpty(fake) then
            local mcc = param:getSetting("cell.operator.mcc." .. i)
            local mnc = param:getSetting("cell.operator.mnc." .. i)
            local msn = param:getSetting("cell.unique.msin." .. i)
            if not (param:stringIsEmpty(mcc) or param:stringIsEmpty(mnc) or param:stringIsEmpty(msn)) then
                fake = mcc .. mnc .. msn
            end
        end

        if not param:stringIsEmpty(fake) and param:isForceSetting(nmeName, res) then
            param:setResult(fake)
            return true, param:safe(res), param:safe(fake)
        end
    end
    return false
end
