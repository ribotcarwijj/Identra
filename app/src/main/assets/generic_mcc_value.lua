function after(hook, param)
    local result = param:getResult()
    if result == nil then
        return false
    end

    local fake = 1 -- test network
    param:setResult(fake)
    return true, result, fake
end
