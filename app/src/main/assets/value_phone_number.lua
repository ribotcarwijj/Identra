function after(hook, param)
    local result = param:getResult()
    if result == nil then
        return false
    end

    local fake = param:getSetting('value.phone_number')
    param:setResult(fake)
    return true, result, fake
end
