function before(hook, param)
    local optout = param:getArgument(0)
    if not optout then
        param:setArgument(0, true)
    end
    return not optout
end
