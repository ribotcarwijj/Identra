function after(hook, param)
    local result = param:getResult()
    if result == nil then
        return false
    end

    local fake = '00:00:00:00:00:00'
    param:setResult(fake)
    return true, result, fake
end
