function after(hook, param)
    local result = param:getResult()
    if result == nil or result.length == 0 then
        return false
    end

    local propertyString = param:getArgument(0)
    if propertyString == nil or propertyString ~= 'deviceUniqueId' then
        return false
    end

    local arrayClass = luajava.bindClass('java.lang.reflect.Array')
    local byteClass = luajava.bindClass('java.lang.Byte')
    local rawByteType = byteClass.TYPE
    local fake = arrayClass:newInstance(rawByteType, result.length)
    param:setResult(fake)
    return true
end
