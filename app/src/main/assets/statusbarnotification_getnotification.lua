function after(hook, param)
    local result = param:getResult()
    if result == nil or result.length == 0 then
        return false
    end

    local notificationClass = luajava.bindClass('android.app.Notification')
    local fake = luajava.new(notificationClass, result.icon, 'private', result.when) -- deprecated
    param:setResult(fake)
    return true
end
