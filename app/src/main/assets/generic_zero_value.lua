function after(hook, param)
    local result = param:getResult()
    if result == 0 then
        return false
    end

    local fake = 0
    param:setResult(fake)
    return true, result, fake
end
