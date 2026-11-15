function before(hook, param)
    local enable = param:getArgument(0)
    if not enable then
        param:setArgument(0, true)
    end
    return not enable
end
