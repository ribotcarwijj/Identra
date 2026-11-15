function after(hook, param)
    local this = param:getThis()
    if this == nil then
        return false
    end

    local result = param:getResult()
    local type = this:getPhoneType()

    local fake
    if type == 1 then -- GSM
        fake = param:getSetting('value.imei')
    end

    if result == nil and fake == nil then
        return false
    end

    param:setResult(fake)
    return true, result, fake
end
