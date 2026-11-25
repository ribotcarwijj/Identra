-- CdmaCellLocation.getBaseStationLatitude() and CellIdentityCdma.getLatitude() return the base station latitude in units of 0.25 arc seconds
-- (degrees * 14400). The setting location.latitude is in decimal degrees, so convert it; passing the
-- raw string made the int coercion fail / truncate.
function before(hook, param)
    local setting = param:getSetting("location.latitude")
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
