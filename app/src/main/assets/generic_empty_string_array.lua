function after(hook, param)
    local result = param:getResult()
    if result == nil or result.length == 0 then
        return false
    end

    local stringClass = luajava.bindClass('java.lang.String')
    local arrayClass = luajava.bindClass('java.lang.reflect.Array')
    local fake = arrayClass:newInstance(stringClass, 0)
    param:setResult(fake)
    return true
end
