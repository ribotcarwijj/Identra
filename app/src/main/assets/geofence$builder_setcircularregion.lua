function before(hook, param)
    param:setArgument(0, 0) -- latitude
    param:setArgument(1, 0) -- longitude
    param:setArgument(2, 0.1) -- radius
    return true
end
