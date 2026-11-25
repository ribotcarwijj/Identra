-- Android carrier id (getSimCarrierId / getSimSpecificCarrierId).
-- The real id names the real carrier, and a random id would not match the fake MCC/MNC,
-- so while a fake operator is set we report UNKNOWN_CARRIER_ID (-1), a value Android itself
-- returns for carriers missing from its carrier database.
function after(hook, param)
    local res = param:getResult()
    if res == nil or res == -1 then
        return false
    end

    local numeric = param:getSetting("cell.operator.numeric.id.1")
    local mcc = param:getSetting("cell.operator.mcc.1")
    if param:stringIsEmpty(numeric) and param:stringIsEmpty(mcc) then
        return false
    end

    local fake = -1
    param:setResult(fake)
    return true, param:safe(res), param:safe(fake)
end
