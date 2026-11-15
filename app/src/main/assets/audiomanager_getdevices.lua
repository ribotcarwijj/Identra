function after(hook, param)
    local array = param:getResult()
    if array == nil or array.length == 0 then
        return false
    end

    local stringClass = luajava.bindClass('android.media.AudioDeviceInfo')
    local arrayClass = luajava.bindClass('java.lang.reflect.Array')
    local fake = arrayClass:newInstance(stringClass, 0)
    param:setResult(fake)
    return true
end
