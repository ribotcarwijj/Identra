-- ISO country of the SIM provider (getSimCountryIso). The optional argument is a subId, not a slot,
-- so always use the first SIM of the profile.
function after(hook, param)
    local res = param:getResult()
    local name = "cell.sim.country.iso.1"
    if not param:isForceSetting(name, res) then
        return false
    end

    local fake = param:getSetting(name)
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
