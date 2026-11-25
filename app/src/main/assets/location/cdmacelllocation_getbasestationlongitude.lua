-- CdmaCellLocation.getBaseStationLongitude() and CellIdentityCdma.getLongitude() return the base station longitude in units of 0.25 arc seconds
-- (degrees * 14400). The setting location.longitude is in decimal degrees, so convert it; passing the
-- raw string made the int coercion fail / truncate.
function before(hook, param)
    local setting = param:getSetting("location.longitude")
    if setting == nil then
        return false
    end

    local deg = tonumber(setting)
    if deg == nil then
        return false
    end

    local fake = math.floor(deg * 14400 + 0.5)
    param:setResult(fake)
    return true, "", tostring(fake)
end
