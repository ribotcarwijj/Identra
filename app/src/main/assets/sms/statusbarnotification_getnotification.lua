function after(hook, param)
    local result = param:getResult()
    if result == nil or result.length == 0 then
        return false
    end

    local setting = param:getSetting("notification.message")
    if setting == nil then
        setting = "private"
    end

    local notificationClass = luajava.bindClass('android.app.Notification')
    local fake = luajava.new(notificationClass, result.icon, setting, result.when) -- deprecated
    param:setResult(fake)
    return true
end