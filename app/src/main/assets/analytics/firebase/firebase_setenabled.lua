function before(hook, param)
    local enabled = param:getArgument(0)
    if enabled then
        param:setArgument(0, false)
    end
    return enabled
end
