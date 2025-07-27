// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract ISTCoin {
    string private _name = "IST Coin";
    string private _symbol = "IST";
    uint8 private _decimals = 2;
    uint256 private _totalSupply = 100_000_000 * 10**2;
    mapping(address => uint256) private _balances;

    constructor() {
        _balances[msg.sender] = _totalSupply; // Mint total supply to deployer
    }

    function name() public view returns (string memory) {
        return _name;
    }

    function symbol() public view returns (string memory) {
        return _symbol;
    }

    function decimals() public view returns (uint8) {
        return _decimals;
    }

    function totalSupply() public view returns (uint256) {
        return _totalSupply;
    }

    function balanceOf(address account) public view returns (uint256) {
        return _balances[account];
    }

    function transfer(address to, uint256 value) public returns (bool) {
        require(to != address(0), "Invalid receiver address");
        require(_balances[msg.sender] >= value, "Insufficient balance");

        _balances[msg.sender] -= value;
        _balances[to] += value;
        return true;
    }
}