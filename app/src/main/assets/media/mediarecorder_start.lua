function before(hook, param)
    local this = param:getThis()
    if this == nil then
        return false
    end

    local source = param:getValue('source', this)
    if source == nil then
        return false
    end

    param:setResult(nil)
    return true
end
