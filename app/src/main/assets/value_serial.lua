function after(hook, param)
    local result = param:getResult()
    if result == nil then
        return false
    end

    local fake = param:getSetting('value.serial')
    if fake == nil then
        fake = 'unknown'
    end

    param:setResult(fake)
    return true, result, fake
end
