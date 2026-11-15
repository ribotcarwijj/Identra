function after(hook, param)
    local result = param:getResult()
    if result == nil then
        return false
    end

    local fake = '00000000-0000-0000-0000-000000000000'
    param:setResult(fake)
    return true, result, fake
end
