-- Carrier / SPN name (getSimOperatorName, getNetworkOperatorName, getSimCarrierIdName, ServiceState alpha names)
function after(hook, param)
    local res = param:getResult()
    local name = "cell.operator.name.1"
    if not param:isForceSetting(name, res) then
        return false
    end

    local fake = param:getSetting(name)
    if param:stringIsEmpty(fake) then
        fake = param:getSetting("cell.display.name.1")
    end
    if param:stringIsEmpty(fake) then
        return false
    end

    if res ~= nil and tostring(res) == fake then
        return false
    end

    param:setResult(fake)
    return true, param:safe(res), param:safe(fake)
end
