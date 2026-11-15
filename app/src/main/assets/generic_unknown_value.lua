function after(hook, param)
    local result = param:getResult()
    if result == nil then
        return false
    end

    local fake = 'unknown'
    param:setResult(fake)
    return true, result, fake
end
