function after(hook, param)
    local res = param:getResult()
    if res == nil then
        return false
    end

    -- only spoof when the setting is configured (getSettingBool fell back to false and forced it for every app)
    if param:getSetting("battery.is.power.save.mode.bool") == nil then
        return false
    end
    local fake = param:getSettingBool("battery.is.power.save.mode.bool", false)
    if fake == res then
        return false
    end
    param:setResult(fake)
    return true, param:safe(res), param:safe(fake)
end