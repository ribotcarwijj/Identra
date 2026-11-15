function before(hook, param)
    local this = param:getThis()
    if this == nil then
        return false
    end

    local source = param:getArgument(0)

    param:putValue('source', source, this)
    return false
end
