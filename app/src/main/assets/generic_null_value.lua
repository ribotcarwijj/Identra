function after(hook, param)
    local result = param:getResult()
    if result == nil then
        return false
    end

    local fake
    param:setResult(fake)
    if result ~= nil and type(result) == 'userdata' then
        result = result:toString()
    end
    return true, result, fake
end
