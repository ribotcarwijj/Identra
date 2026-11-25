function after(hook, param)
    local res = param:getResult()
    if res == nil then
        return false
    end

    local fake = param:getSettingInt("hardware.gps.model.year", 2018)
    if fake == nil then
        -- getSettingInt returns nil when the setting is missing; setResult(nil) on an int method throws in the app
        return false
    end
    param:setResult(fake)
    return true, tostring(res), tostring(fake)
end