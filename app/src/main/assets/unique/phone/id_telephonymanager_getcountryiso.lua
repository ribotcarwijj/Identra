-- ISO country of the registered network (getNetworkCountryIso / getNetworkCountryIso(slotIndex)).
-- Android reports it in lower case; skip when the profile has no value for this slot.
function after(hook, param)
    local res = param:getResult()
    local index = param:tryGetArgumentInt(0, 0)
    local settingIndex = index + 1
    local nmeName = "cell.sim.country.iso." .. tostring(settingIndex)
    if not param:isForceSetting(nmeName, res) then
        return false
    end

    local fake = param:getSetting(nmeName)
    if param:stringIsEmpty(fake) then
        return false
    end
    fake = string.lower(fake)

    if res ~= nil and tostring(res) == fake then
        return false
    end

    param:setResult(fake)
    return true, param:safe(res), param:safe(fake)
end
