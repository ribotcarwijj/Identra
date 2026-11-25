-- NetworkInfo.getExtraInfo(): for Wi-Fi this is the quoted SSID of the connected network, which leaked
-- the real SSID while WifiInfo was spoofed. Replace it with the profile SSID (unique.network.ssid).
-- Mobile networks (APN name) and "<unknown ssid>" (no location permission) are left alone.
function after(hook, param)
    local res = param:getResult()
    if res == nil then
        return false
    end

    local ths = param:getThis()
    if ths == nil or ths:getType() ~= 1 then -- ConnectivityManager.TYPE_WIFI
        return false
    end

    local current = tostring(res)
    if current == "" or current == "<unknown ssid>" then
        return false
    end

    local ssid = param:getSetting("unique.network.ssid")
    if param:stringIsEmpty(ssid) then
        return false
    end
    ssid = string.gsub(ssid, '^"(.*)"$', '%1')

    local fake = '"' .. ssid .. '"'
    if current == fake then
        return false
    end

    param:setResult(fake)
    return true, param:safe(current), param:safe(fake)
end
